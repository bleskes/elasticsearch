/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 7953 7243 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/

package com.prelert.job.manager;


import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prelert.job.normalisation.Normaliser;
import com.prelert.job.persistence.JobProvider;
import com.prelert.job.process.MissingFieldException;
import com.prelert.job.process.NativeProcessRunException;
import com.prelert.job.process.ProcessManager;
import com.prelert.job.process.ResultsReaderFactory;
import com.prelert.job.usage.UsageReporterFactory;
import com.prelert.job.warnings.HighProportionOfBadTimestampsException;
import com.prelert.job.warnings.OutOfOrderRecordsException;
import com.prelert.job.warnings.StatusReporterFactory;
import com.prelert.job.AnalysisConfig;
import com.prelert.job.JobIdAlreadyExistsException;
import com.prelert.job.JobConfiguration;
import com.prelert.job.JobConfigurationException;
import com.prelert.job.JobDetails;
import com.prelert.job.JobInUseException;
import com.prelert.job.JobStatus;
import com.prelert.job.TooManyJobsException;
import com.prelert.job.UnknownJobException;
import com.prelert.rs.data.AnomalyRecord;
import com.prelert.rs.data.Bucket;
import com.prelert.rs.data.ErrorCode;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;


/**
 * Creates jobs and handles retrieving job configuration details from
 * the data store. New jobs have a unique job id see {@linkplain #generateJobId()}
 */
public class JobManager 
{
	static public final Logger s_Logger = Logger.getLogger(JobManager.class);

	/**
	 * Field name in which to store the API version in the usage info
	 */
	static public final String APP_VER_FIELDNAME = "appVer";
	
	/**
	 * The default number of documents returned in queries as a string.
	 */
	static public final String DEFAULT_PAGE_SIZE_STR = "100";
	
	/**
	 * The default number of documents returned in queries. 
	 */
	static public final int DEFAULT_PAGE_SIZE;
	static
	{
		DEFAULT_PAGE_SIZE = Integer.parseInt(DEFAULT_PAGE_SIZE_STR);
	}
	
	static public final String DEFAULT_RECORD_SORT_FIELD = AnomalyRecord.PROBABILITY;
	
	private ProcessManager m_ProcessManager;
	 
	
	private AtomicLong m_IdSequence;	
	private DateFormat m_JobIdDateFormat;
	
	private ObjectMapper m_ObjectMapper;
	
	private JobProvider m_JobProvider;
	
	
	private Map<String, Integer> m_JobIdBucketspan;

	/**
	 * These default to unlimited (indicated by negative limits), but may be
	 * overridden by constraints in the license key.
	 */
	private int m_MaxActiveJobs = -1;
	private int m_MaxDetectorsPerJob = -1;
	private int m_MaxPartitionsPerJob = -1;

	/**
	 * constraints in the license key.
	 */
	static public final String JOBS_LICENSE_CONSTRAINT = "jobs";
	static public final String DETECTORS_LICENSE_CONSTRAINT = "detectors";
	static public final String PARTITIONS_LICENSE_CONSTRAINT = "partitions";

	/**
	 * Create a JobManager
	 * 
	 * @param jobDetailsProvider 
	 */
	public JobManager(JobProvider jobProvider,
			ResultsReaderFactory resultsReaderFactory,
			StatusReporterFactory statusReporterFactory,
			UsageReporterFactory usageReporterFactory)
	{
		m_JobIdBucketspan = new HashMap<>();
		
		m_JobProvider = jobProvider;
		
		m_ProcessManager = new ProcessManager(jobProvider, 
				resultsReaderFactory, statusReporterFactory,
				usageReporterFactory);
		
		m_IdSequence = new AtomicLong();		
		m_JobIdDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
				 
		m_ObjectMapper = new ObjectMapper();
		m_ObjectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

		// This requires the process manager and ElasticSearch connection in
		// order to work, but failure is considered non-fatal
		saveInfo();		
	}	
		
	
	/**
	 * Get the details of the specific job wrapped in a <code>SingleDocument</code>
	 * 
	 * @param jobId
	 * @return The JobDetails or throws UnknownJobException
	 */
	public SingleDocument<JobDetails> getJob(String jobId)
	throws UnknownJobException
	{
		SingleDocument<JobDetails> doc = new SingleDocument<>();
		doc.setType(JobDetails.TYPE);
		doc.setDocumentId(jobId);

		doc.setDocument(m_JobProvider.getJobDetails(jobId));
		doc.setExists(doc.getDocument() != null);
		
		return doc;
	}

	/**
	 * Get details of all Jobs.
	 * 
	 * @param skip Skip the first N Jobs. This parameter is for paging
	 * results if not required set to 0.
	 * @param take Take only this number of Jobs
	 * @return A pagination object with hitCount set to the total number  
	 * of jobs not the only the number returned here as determined by the 
	 * <code>take</code>
	 * parameter.
	 */
	public Pagination<JobDetails> getJobs(int skip, int take)
	{
		return m_JobProvider.getJobs(skip, take);
	}

	/**
	 * Create a new job from the configuration object and insert into the 
	 * document store. The details of the newly created job are returned.
	 *  
	 * @param jobConfig
	 * @return The new job or <code>null</code> if an exception occurs.
	 * @throws UnknownJobException
	 * @throws IOException 
	 * @throws TooManyJobsException If the license is violated
	 * @throws JobConfigurationException If the license is violated
	 * @throws JobIdAlreadyExistsException If the alias is already taken
	 */
	public JobDetails createJob(JobConfiguration jobConfig)
	throws UnknownJobException, IOException, TooManyJobsException, 
		JobConfigurationException, JobIdAlreadyExistsException
	{
		// Negative m_MaxActiveJobs means unlimited
		if (m_MaxActiveJobs >= 0 &&
			m_ProcessManager.numberOfRunningJobs() >= m_MaxActiveJobs)
		{
			throw new TooManyJobsException(m_MaxActiveJobs,
					"Cannot create new job - your license limits you to " +
					m_MaxActiveJobs + " concurrently running " +
					(m_MaxActiveJobs == 1 ? "job" : "jobs") +
					".  You must close a job before you can create a new one.",
					ErrorCode.LICENSE_VIOLATION);
		}

		// Negative m_MaxDetectorsPerJob means unlimited
		if (m_MaxDetectorsPerJob >= 0 &&
			jobConfig.getAnalysisConfig().getDetectors().size() > m_MaxDetectorsPerJob)
		{
			throw new JobConfigurationException(
					"Cannot create new job - your license limits you to " +
					m_MaxDetectorsPerJob +
					(m_MaxDetectorsPerJob == 1 ? " detector" : " detectors") +
					" per job, but you have configured " +
					jobConfig.getAnalysisConfig().getDetectors().size(),
					ErrorCode.LICENSE_VIOLATION);
		}

		// We can only validate the case of m_MaxPartitionsPerJob being zero in
		// the Java code - anything more subtle has to be left to the C++
		if (m_MaxPartitionsPerJob == 0)
		{
			for (com.prelert.job.Detector detector :
						jobConfig.getAnalysisConfig().getDetectors())
			{
				String partitionFieldName = detector.getPartitionFieldName();
				if (partitionFieldName != null &&
					partitionFieldName.length() > 0)
				{
					throw new JobConfigurationException(
							"Cannot create new job - your license disallows" +
							" partition fields, but you have configured one.",
							ErrorCode.LICENSE_VIOLATION);
				}
			}
		}

		String jobId = jobConfig.getId();
		if (jobId == null || jobId.isEmpty())
		{
			jobId = generateJobId();
		}
		else
		{
			m_JobProvider.jobIdIsUnique(jobId);
		}
		
		JobDetails jobDetails;
		
		if (jobConfig.getReferenceJobId() != null && 
				jobConfig.getReferenceJobId().isEmpty() == false)
		{
			JobDetails referenced = getReferencedJob(jobConfig.getReferenceJobId());
			jobDetails = new JobDetails(jobId, referenced, jobConfig);
		}
		else
		{
			jobDetails = new JobDetails(jobId, jobConfig);
		}
			
		m_JobProvider.createJob(jobDetails);
		
		return jobDetails;
		
	}

	/**
	 * Get a single result bucket
	 * 
	 * @param jobId
	 * @param bucketId
	 * @param expand Include anomaly records
	 * @return
	 * @throws NativeProcessRunException 
	 */
	public SingleDocument<Bucket> bucket(String jobId, 
			String bucketId, boolean expand, String normalisationType) 
	throws NativeProcessRunException
	{
		if (normalisationType.equals("u"))
		{
			expand = true;
		}
		
		SingleDocument<Bucket> bucket = m_JobProvider.bucket(jobId, bucketId, expand);
		
		if (bucket.isExists())
		{
			return normalise(jobId, bucket, normalisationType);
		}
		else 
		{
			return bucket;
		}
	}
	
	/**
	 * Get result buckets
	 * 
	 * @param jobId
	 * @param expand Include anomaly records
	 * @param skip
	 * @param take
	 * @return
	 */
	public Pagination<Bucket> buckets(String jobId, 
			boolean expand, int skip, int take, String normalisationType)
	throws NativeProcessRunException
	{
		if (normalisationType.equals("u"))
		{
			expand = true;
		}
		
		Pagination<Bucket> buckets = m_JobProvider.buckets(jobId, expand, skip, take);
		
		
		return normalise(jobId, buckets, normalisationType);
	}
	
	
	/**
	 * Get result buckets between 2 dates 
	 * @param jobId
	 * @param expand
	 * @param skip
	 * @param take
	 * @param startBucket The bucket with this id is included in the results
	 * @param endBucket Include buckets up to this one
	 * @return
	 */
	public Pagination<Bucket> buckets(String jobId, 
			boolean expand, int skip, int take, long startBucket, long endBucket,
			String normalisationType)
	throws NativeProcessRunException
	{
		if (normalisationType.equals("u"))
		{
			expand = true;
		}
		
		Pagination<Bucket> buckets =  m_JobProvider.buckets(jobId, 
				expand, skip, take, 
				startBucket, endBucket);
		
		return normalise(jobId, buckets, normalisationType);
	}
	
	private Integer getJobBucketSpan(String jobId)
	{
		Integer span = m_JobIdBucketspan.get(jobId);
		if (span == null)
		{
			// use dot notation to get fields from nested docs.
			Number num = m_JobProvider.<Number>getField(jobId,
					JobDetails.ANALYSIS_CONFIG + "." + AnalysisConfig.BUCKET_SPAN);
			
			if (num != null)
			{
				span = num.intValue();
				m_JobIdBucketspan.put(jobId, span);
			}			
		}
		
		return span;
	}
	
		
	private Pagination<Bucket> normalise(String jobId,
			Pagination<Bucket> buckets, String normalisationType) 
	throws NativeProcessRunException
	{
		Normaliser normaliser = new Normaliser(jobId, m_JobProvider);
		
		if (normalisationType != null && normalisationType.equals("u"))
		{
			normaliser.normaliseForUnusualBehaviour(getJobBucketSpan(jobId), 
					buckets.getDocuments());
		}
		else
		{
			normaliser.normaliseForSystemChange(getJobBucketSpan(jobId), 
				buckets.getDocuments());
		}
			
		return buckets;
	}
	
	
	private SingleDocument<Bucket> normalise(String jobId,
			SingleDocument<Bucket> bucket, String normalisationType) 
	throws NativeProcessRunException
	{
		Normaliser normaliser = new Normaliser(jobId, m_JobProvider);
		
		if (normalisationType != null && normalisationType.equals("u"))
		{
			normaliser.normaliseForUnusualBehaviour(getJobBucketSpan(jobId), 
					Arrays.asList(new Bucket [] {bucket.getDocument()}));
		}
		else
		{
			normaliser.normaliseForSystemChange(getJobBucketSpan(jobId), 
					Arrays.asList(new Bucket [] {bucket.getDocument()}));
		}
			
		return bucket;
	}
	

	/**
	 * Get the anomaly records for the bucket. 
	 * Does not include simple count records.
	 * Records are sorted by probability  
	 * 
	 * @param jobId
	 * @param bucketId 
	 * @param skip Skip the first N records. This parameter is for paging
	 * results if not required set to 0.
	 * @param take Take only this number of records
	 * @return
	 * @throws NativeProcessRunException 
	 */
	public Pagination<AnomalyRecord> records(String jobId, 
			String bucketId, int skip, int take) 
	throws NativeProcessRunException 
	{
		return this.records(jobId, bucketId, skip, take, 
				DEFAULT_RECORD_SORT_FIELD, "both");
	}
	
	/**
	 * Get the anomaly records for the bucket. 
	 * Does not include simple count records.
	 * 
	 * @param jobId
	 * @param bucketId 
	 * @param skip Skip the first N records. This parameter is for paging
	 * results if not required set to 0.
	 * @param take Take only this number of records
	 * @param sortField The field to sort the anomaly records by
	 * @param norm Normalisation type
	 * @return
	 * @throws NativeProcessRunException 
	 */
	public Pagination<AnomalyRecord> records(String jobId, 
			String bucketId, int skip, int take, String sortField, String norm) 
	throws NativeProcessRunException 
	{
		Pagination<AnomalyRecord> records = m_JobProvider.records(jobId, 
				bucketId, false, skip, take, sortField);
		
		SingleDocument<Bucket> bucket = m_JobProvider.bucket(jobId, bucketId, false); 
		
		Normaliser normaliser = new Normaliser(jobId, m_JobProvider);	

		boolean normaliseBoth = "both".equals(norm);
		normaliser.normaliseForBoth(getJobBucketSpan(jobId), 
					Arrays.asList(new Bucket[] {bucket.getDocument()}),
					records.getDocuments(), normaliseBoth);		
		
		return records; 
	}
	
	/**
	 * Get a page of anomaly records from the buckets between
	 * epochStart and epochEnd. Does not include simple count records.
	 * Records are sorted by probability  
	 * 
	 * @param jobId
	 * @param skip
	 * @param take
	 * @param epochStart
	 * @param epochEnd
	 * @return
	 * @throws NativeProcessRunException
	 */
	public Pagination<AnomalyRecord> records(String jobId, 
			int skip, int take, long epochStart, long epochEnd) 
	throws NativeProcessRunException 
	{
		return records(jobId, skip, take, epochStart, epochEnd, 
				DEFAULT_RECORD_SORT_FIELD, "both");
	}
	
	/**
	 * Get a page of anomaly records from the buckets between
	 * epochStart and epochEnd. Does not include simple count records.
	 * 
	 * @param jobId
	 * @param skip
	 * @param take
	 * @param epochStart
	 * @param epochEnd
	 * @param sortField
	 * @param norm
	 * @return
	 * @throws NativeProcessRunException
	 */
	public Pagination<AnomalyRecord> records(String jobId, 
			int skip, int take, long epochStart, long epochEnd, String sortField,
			String norm) 
	throws NativeProcessRunException 
	{
		Pagination<AnomalyRecord> records = m_JobProvider.records(jobId, 
				false, skip, take, epochStart, epochEnd, sortField);
		
		Pagination<Bucket> buckets = m_JobProvider.buckets(jobId, 
				false, skip, take, epochStart, epochEnd);
		
		Normaliser normaliser = new Normaliser(jobId, m_JobProvider);	
		
		boolean normaliseBoth = "both".equals(norm);
		normaliser.normaliseForBoth(getJobBucketSpan(jobId), 
					buckets.getDocuments(), records.getDocuments(), normaliseBoth);
		
		return records; 
	}
	
	/**
	 * Get a page of anomaly records from all buckets.
	 * Does not include simple count records.
	 * 
	 * @param jobId
	 * @param skip Skip the first N records. This parameter is for paging
	 * results if not required set to 0.
	 * @param take Take only this number of records
	 * @return
	 * @throws NativeProcessRunException
	 */
	public Pagination<AnomalyRecord> records(String jobId, 
			int skip, int take) 
	throws NativeProcessRunException 
	{
		return records(jobId, skip, take, DEFAULT_RECORD_SORT_FIELD, "both");
	}
	
	
	/**
	 * Get a page of anomaly records from all buckets.
	 * Does not include simple count records.
	 * 
	 * @param jobId
	 * @param skip Skip the first N records. This parameter is for paging
	 * results if not required set to 0.
	 * @param take Take only this number of records
	 * @param sortField The field to sort by
	 * @param norm The normalisation type
	 * @return
	 * @throws NativeProcessRunException
	 */
	public Pagination<AnomalyRecord> records(String jobId, 
			int skip, int take, String sortField, String norm) 
	throws NativeProcessRunException 
	{
		Pagination<AnomalyRecord> records = m_JobProvider.records(jobId, 
				false, skip, take, sortField);
		
		if (records.getHitCount() == 0)
		{
			return records;
		}
		
		// get the parent bucket ids and sort
		List<String> bucketIds = new ArrayList<>();
		for (AnomalyRecord r : records.getDocuments())
		{
			bucketIds.add(r.getParent());
		}
		Collections.sort(bucketIds);
		
		// get all the buckets over the same time period
		// as the records
		try
		{			
			long start = Long.parseLong(bucketIds.get(0));
			// we want the last bucket inclusive so +1 to the value
			long end = Long.parseLong(bucketIds.get(bucketIds.size() -1)) + 1;

			int bucketSkip = 0;
			Pagination<Bucket> buckets = m_JobProvider.buckets(jobId, 
					false, bucketSkip, take, start, end);
			bucketSkip += take;
			while (bucketSkip < buckets.getHitCount())
			{
				Pagination<Bucket> extraBuckets = m_JobProvider.buckets(
						jobId, false, bucketSkip, take, start, end);
				
				bucketSkip += take;
				buckets.getDocuments().addAll(extraBuckets.getDocuments());
			}

			Normaliser normaliser = new Normaliser(jobId, m_JobProvider);	

			boolean normaliseBoth = "both".equals(norm);
			normaliser.normaliseForBoth(getJobBucketSpan(jobId), 
					buckets.getDocuments(), records.getDocuments(), normaliseBoth);
		}
		catch (NumberFormatException nfe)
		{
			s_Logger.error("Error parsing record parent id", nfe);
		}
		
		return records; 
	}
	
	
	/**
	 * Set the job's description.
	 * If the description cannot be set an exception is thrown.
	 * 
	 * @param jobId
	 * @param description
	 * @throws UnknownJobException
	 */
	public void setDescription(String jobId, String description)
	throws UnknownJobException
	{
		Map<String, Object> update = new HashMap<>();
		update.put(JobDetails.DESCRIPTION, description);
		m_JobProvider.updateJob(jobId, update);
	}
	
	/**
	 * Stop the running job and mark it as finished.<br/>
	 * 
	 * @param jobId The job to stop
	 * @return true if the job stopped successfully
	 * @throws UnknownJobException 
	 * @throws NativeProcessRunException 
	 * @throws JobInUseException if the job cannot be closed because data is
	 * being streamed to it
	 */
	public boolean finishJob(String jobId) 
	throws UnknownJobException, NativeProcessRunException, JobInUseException
	{
		s_Logger.debug("Finish job " + jobId);
		
		// First check the job is in the database.
		// this method throws if it isn't
		if (m_JobProvider.jobExists(jobId))
		{
			ProcessManager.ProcessStatus processStatus = m_ProcessManager.finishJob(jobId);	
			return (processStatus == ProcessManager.ProcessStatus.COMPLETED);
		}
		
		return false;
	}
		
	/**
	 * Set time the job last received data.
	 * Updates the database document
	 * 
	 * @param jobId
	 * @param time
	 * @return
	 * @throws UnknownJobException 
	 */
	private boolean updateLastDataTime(String jobId, Date time) 
	throws UnknownJobException
	{
		
		Map<String, Object> update = new HashMap<>();
		update.put(JobDetails.LAST_DATA_TIME, new Date());
		update.put(JobDetails.STATUS, JobStatus.RUNNING);
		return m_JobProvider.updateJob(jobId, update);	
	}
	
	/**
	 * Stop the associated process and remove it from the Process
	 * Manager then delete the job related documents from the 
	 * database.
	 * 
	 * @param jobId
	 * @return
	 * @throws UnknownJobException If the jobId is not recognised
	 * @throws NativeProcessRunException 
	 * @throws JobInUseException If the job cannot be deleted because the
	 * native process is in use.
	 */
	public boolean deleteJob(String jobId)
	throws UnknownJobException, NativeProcessRunException, JobInUseException
	{		
		s_Logger.debug("Deleting job '" + jobId + "'");
		
		m_JobProvider.jobExists(jobId);
		
		m_ProcessManager.finishJob(jobId);
		m_JobProvider.deleteJob(jobId);
		
		return true;
	}
	
	/**
	 * Passes data to the native process. If the process is not running a new 
	 * one is started. 
	 * This is a blocking call that won't return until all the data has been 
	 * written to the process. A new thread is launched to parse the process's 
	 * output
	 * 
	 * @param jobId
	 * @param input
	 * @return
	 * @throws NativeProcessRunException If there is an error starting the native 
	 * process
	 * @throws UnknownJobException If the jobId is not recognised
	 * @throws MissingFieldException If a configured field is missing from 
	 * the CSV header
	 * @throws JsonParseException 
	 * @throws JobInUseException if the job cannot be written to because 
	 * it is already handling data
	 * @throws HighProportionOfBadTimestampsException 
	 * @throws OutOfOrderRecordsException 
	 * @throws TooManyJobsException If the license is violated
	 */
	public boolean dataToJob(String jobId, InputStream input) 
	throws UnknownJobException, NativeProcessRunException, MissingFieldException, 
		JsonParseException, JobInUseException, HighProportionOfBadTimestampsException,
		OutOfOrderRecordsException, TooManyJobsException
	{
		// Negative m_MaxActiveJobs means unlimited
		if (m_MaxActiveJobs >= 0 &&
			m_ProcessManager.jobIsRunning(jobId) &&
			m_ProcessManager.numberOfRunningJobs() >= m_MaxActiveJobs)
		{
			throw new TooManyJobsException(m_MaxActiveJobs,
					"Cannot reactivate job with id '" + jobId +
					"' - your license limits you to " + m_MaxActiveJobs +
					" concurrently running jobs.  You must close a job before" +
					" you can reactivate a closed one.",
					ErrorCode.LICENSE_VIOLATION);
		}

		try
		{
			if (m_ProcessManager.dataToJob(jobId, input) == false)
			{
				return false;
			}
		}
		catch (NativeProcessRunException ne)
		{
			try
			{
				m_ProcessManager.finishJob(jobId);
			}
			catch (NativeProcessRunException e)
			{
				s_Logger.warn("Error finished job after dataToJob failed", e);
			}

			// rethrow
			throw ne;
		}

		updateLastDataTime(jobId, new Date()); 
		
		return true;
	}
	
	
	/**
	 * The job id is a concatenation of the date in 'yyyyMMddHHmmss' format 
	 * and a sequence number that is a minimum of 5 digits wide left padded
	 * with zeros.<br/>
	 * e.g. the first Id created 23rd November 2013 at 11am 
	 * 	'20131125110000-00001' 
	 * 
	 * @return The new unique job Id
	 */
	private String generateJobId()
	{
		String id = String.format("%s-%05d", m_JobIdDateFormat.format(new Date()),
						m_IdSequence.incrementAndGet());		
		return id;
	}		
		
	/**
	 * Stops the ElasticSearch client and the Process Manager
	 */
	public void stop()
	{
		m_ProcessManager.stop();
		try 
		{
			m_JobProvider.close();
		}
		catch (IOException e) 
		{
			s_Logger.error("Exception closing job details provider", e);
		}
	}
	
	/**
	 * Get the Job details for the job Id. If the job cannot be found
	 * <code>null</code> is returned.
	 *  
	 * @param refId
	 * @return <code>null</code> or the job details
	 * @throws UnknownJobException If there is no previously created
	 * job with the id <code>refId</code>
	 */
	private JobDetails getReferencedJob(String refId)
	throws UnknownJobException
	{
		try
		{
			return m_JobProvider.getJobDetails(refId);
		}
		catch (UnknownJobException e)
		{
			throw new UnknownJobException(refId, "Missing Job: Cannot find "
					+ "referenced job with id '" + refId + "'",
					ErrorCode.UNKNOWN_JOB_REFERENCE);
	
		}
	}
	
	/**
	 * Get the analytics version string.
	 * 
	 * @return
	 */
	public String getAnalyticsVersion()
	{
		return  m_ProcessManager.getAnalyticsVersion();
	}

	/**
	 * Get the limit on number of active jobs.
	 * A negative limit means unlimited.
	 */
	public int getMaxActiveJobs()
	{
		return m_MaxActiveJobs;
	}

	/**
	 * Get the limit on number of detectors per job.
	 * A negative limit means unlimited.
	 */
	public int getMaxDetectorsPerJob()
	{
		return m_MaxDetectorsPerJob;
	}

	/**
	 * Get the limit on number of partitions per job.
	 * A negative limit means unlimited.
	 * Note that the Java code can really only do anything with this if it's
	 * zero, as it doesn't count the number of distinct values of the partition
	 * field.  However, if the limit is zero it can reject any configured
	 * partition field settings.
	 */
	public int getMaxPartitionsPerJob()
	{
		return m_MaxPartitionsPerJob;
	}


	/**
	 * Attempt to get usage and license info from the C++ process, add extra
	 * fields and persist to ElasticSearch.  Any failures are logged but do not
	 * otherwise impact operation of this process.  Additionally, any license
	 * constraints are extracted from the same info document.
	 */
	private void saveInfo()
	{
		// This will be a JSON document in string form
		String backendInfo = m_ProcessManager.getInfo();

		// Try to parse the string returned from the C++ process and extract
		// any license constraints
		ObjectNode doc;
		try
		{
			doc = (ObjectNode)m_ObjectMapper.readTree(backendInfo);

			// Negative numbers indicate no constraint, i.e. unlimited maximums
			JsonNode constraint = doc.get(JOBS_LICENSE_CONSTRAINT);
			if (constraint != null)
			{
				m_MaxActiveJobs = constraint.asInt(-1);
			}
			else
			{
				m_MaxActiveJobs = -1;
			}
			s_Logger.info("Max active jobs = " + m_MaxActiveJobs);
			constraint = doc.get(DETECTORS_LICENSE_CONSTRAINT);
			if (constraint != null)
			{
				m_MaxDetectorsPerJob = constraint.asInt(-1);
			}
			else
			{
				m_MaxDetectorsPerJob = -1;
			}
			s_Logger.info("Max detectors per job = " + m_MaxDetectorsPerJob);
			constraint = doc.get(PARTITIONS_LICENSE_CONSTRAINT);
			if (constraint != null)
			{
				m_MaxPartitionsPerJob = constraint.asInt(-1);
			}
			else
			{
				m_MaxPartitionsPerJob = -1;
			}
			s_Logger.info("Max partitions per job = " + m_MaxPartitionsPerJob);
		}
		catch (IOException e)
		{
			s_Logger.warn("Failed to parse JSON document " + backendInfo, e);
			return;
		}
		catch (ClassCastException e)
		{
			s_Logger.warn("Parsed non-object JSON document " + backendInfo, e);
			return;
		}

		// Try to add extra fields (just appVer for now)
		try
		{
			Properties props = new Properties();
			// Try to get the API version as recorded by Maven at build time
			InputStream is = getClass().getResourceAsStream("/META-INF/maven/com.prelert/engineApi/pom.properties");
			if (is != null)
			{
				props.load(is);
			}
			doc.put(APP_VER_FIELDNAME, props.getProperty("version"));
		}
		catch (IOException e)
		{
			s_Logger.warn("Failed to load API version meta-data", e);
			return;
		}
		catch (IllegalArgumentException e)
		{
			s_Logger.warn("Malformed API version meta-data", e);
			return;
		}

		// Try to persist the modified document
		try
		{
			m_JobProvider.savePrelertInfo(doc.toString());
		}
		catch (Exception e)
		{
			s_Logger.warn("Error writing Prelert info to ElasticSearch", e);
			return;
		}

		s_Logger.info("Wrote Prelert info " + doc.toString() + " to ElasticSearch");
	}
	

}
