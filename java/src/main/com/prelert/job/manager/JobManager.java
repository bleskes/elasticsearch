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

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.RangeFilterBuilder;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.node.Node;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortOrder;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prelert.job.persistence.elasticsearch.ElasticSearchMappings;
import com.prelert.job.persistence.elasticsearch.ElasticSearchPersister;
import com.prelert.job.persistence.elasticsearch.ElasticSearchResultsReaderFactory;
import com.prelert.job.process.JobDetailsProvider;
import com.prelert.job.process.MissingFieldException;
import com.prelert.job.process.NativeProcessRunException;
import com.prelert.job.process.ProcessManager;
import com.prelert.job.DetectorState;
import com.prelert.job.JobConfiguration;
import com.prelert.job.JobDetails;
import com.prelert.job.JobInUseException;
import com.prelert.job.JobStatus;
import com.prelert.job.UnknownJobException;
import com.prelert.rs.data.AnomalyRecord;
import com.prelert.rs.data.Bucket;
import com.prelert.rs.data.Detector;
import com.prelert.rs.data.ErrorCodes;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;


/**
 * Creates jobs and handles retrieving job configuration details from
 * the data store. New jobs have a unique job id see {@linkplain #generateJobId()}
 */
public class JobManager implements JobDetailsProvider
{
	static public final Logger s_Logger = Logger.getLogger(JobManager.class);

	/**
	 * Field name in which to store the API version in the usage info
	 */
	static public final String APP_VER_FIELDNAME = "appVer";

	/**
	 * Where to store the usage info in ElasticSearch - must match what's
	 * expected by kibana/engineAPI/app/directives/prelertLogUsage.js
	 */
	static public final String USAGE_INFO_INDEX = "prelert-int";
	static public final String USAGE_INFO_TYPE = "usage";
	static public final String USAGE_INFO_ID = "usageStats";

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

	
	private Node m_Node;
	private Client m_Client;
	
	private ProcessManager m_ProcessManager;
	
	private AtomicLong m_IdSequence;	
	private DateFormat m_DateFormat;
	
	private ObjectMapper m_ObjectMapper;
	
	/**
	 * Create a JobManager and a default Elasticsearch node
	 * on localhost with the default port.
	 * 
	 * @param elasticSearchClusterName The name of the ElasticSearch cluster
	 */
	public JobManager(String elasticSearchClusterName)
	{
		this(nodeBuilder().client(true).clusterName(elasticSearchClusterName).node());

		s_Logger.info("Connecting to ElasticSearch cluster '" 
				+ elasticSearchClusterName + "'");
	}	
		
	/**
	 * Create a JobManager with the given ElasticSearch node, clients
	 * will be created on that node.
	 *  
	 * @param node The ElasticSearch node
	 */
	public JobManager(Node node)
	{
		m_Node = node;
		m_Client = m_Node.client();
		
		m_ProcessManager = new ProcessManager(this, 
				new ElasticSearchResultsReaderFactory(m_Node));
		
		m_IdSequence = new AtomicLong();		
		m_DateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
				 
		m_ObjectMapper = new ObjectMapper();
		m_ObjectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

		// This requires the process manager and ElasticSearch connection in
		// order to work, but failure is considered non-fatal
		saveUsageInfo();
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

		doc.setDocument(this.getJobDetails(jobId));
		doc.setExists(doc.getDocument() != null);
		
		return doc;
	}
	
	/**
	 * Get the details of the specific job 
	 * 
	 * @param jobId
	 * @return The JobDetails or throws UnknownJobException
	 * @throws UnknownJobException if the job details document cannot be read
	 */
	@Override
	public JobDetails getJobDetails(String jobId) 
	throws UnknownJobException 
	{
		try
		{
			GetResponse response = m_Client.prepareGet(jobId, JobDetails.TYPE, jobId).get();
			if (response.isExists())
			{
				return new JobDetails(response.getSource()); 			
			}		
			else 
			{
				String msg = "No details for job with id " + jobId;
				s_Logger.warn(msg);
				throw new UnknownJobException(jobId, msg,
						ErrorCodes.MISSING_JOB_ERROR);
			}
		}
		catch (IndexMissingException e)
		{
			// the job does not exist
			String msg = "Missing Index no job with id " + jobId;
			s_Logger.warn(msg);
			throw new UnknownJobException(jobId, msg, 
					ErrorCodes.MISSING_JOB_ERROR);
		}
	}
	
		
	/**
	 * Get details of all Jobs.
	 * Searches across all job indexes for job documents
	 * 
	 * @param skip Skip the first N Jobs. This parameter is for paging
	 * results if not required set to 0.
	 * @param take Take only this number of Jobs
	 * @return
	 */
	public Pagination<JobDetails> getAllJobs(int skip, int take)
	{
		FilterBuilder fb = FilterBuilders.matchAllFilter();
		SortBuilder sb = new FieldSortBuilder(JobDetails.ID)
							.ignoreUnmapped(true)
							.missing("_last")
							.order(SortOrder.DESC);

		SearchResponse response = m_Client.prepareSearch("_all")
				.setTypes(JobDetails.TYPE)
				.setPostFilter(fb)
				.setFrom(skip).setSize(take)
				.addSort(sb)
				.get();

		List<JobDetails> jobs = new ArrayList<>();
		for (SearchHit hit : response.getHits().getHits())
		{
			jobs.add(new JobDetails(hit.getSource())); 
		}
		
		Pagination<JobDetails> page = new Pagination<>();
		page.setDocuments(jobs);
		page.setHitCount(response.getHits().getTotalHits());
		page.setSkip(skip);
		page.setTake(take);
		page.setAllResults(response.getHits().getHits().length == page.getHitCount());
		
		return page;
	}
	
	/**
	 * Create a new job from the configuration object and insert into the 
	 * document store. The details of the newly created job are returned.
	 *  
	 * @param jobConfig
	 * @return The new job or <code>null</code> if an exception occurs.
	 * @throws UnknownJobException
	 * @throws JsonProcessingException 
	 */
	public JobDetails createJob(JobConfiguration jobConfig)
	throws UnknownJobException , JsonProcessingException
	{
		String jobId = generateJobId();
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
	
		try		
		{
			XContentBuilder jobMapping = ElasticSearchMappings.jobMapping();
			XContentBuilder bucketMapping = ElasticSearchMappings.bucketMapping();
			XContentBuilder detectorMapping = ElasticSearchMappings.detectorMapping();
			XContentBuilder recordMapping = ElasticSearchMappings.recordMapping();
			XContentBuilder detectorStateMapping = ElasticSearchMappings.detectorStateMapping();
			
			
			m_Client.admin().indices()
					.prepareCreate(jobId)					
					.addMapping(JobDetails.TYPE, jobMapping)
					.addMapping(Bucket.TYPE, bucketMapping)
					.addMapping(Detector.TYPE, detectorMapping)
					.addMapping(AnomalyRecord.TYPE, recordMapping)
					.addMapping(DetectorState.TYPE, detectorStateMapping)
					.get();
								
			String json = m_ObjectMapper.writeValueAsString(jobDetails);
			
			m_Client.prepareIndex(
					jobId, JobDetails.TYPE, jobId)
					.setSource(json)
					.get();
						
			return jobDetails;
		}
		catch (IOException e)
		{
			s_Logger.error("Error writing ElasticSearch mappings", e);
		}

		return null;
	}
	
	/**
	 * Get all the result buckets for the job id
	 * 
	 * @param jobId
	 * @param expand Include anomaly records
	 * @param skip Skip the first N Buckets. This parameter is for paging
	 * if not required set to 0.
	 * @param take Take only this number of Buckets
	 * @return
	 */
	public Pagination<Map<String, Object>> buckets(String jobId, 
			boolean expand, int skip, int take)
	{
		FilterBuilder fb = FilterBuilders.matchAllFilter();
		
		return buckets(jobId, expand, skip, take, fb);
	}
	
	/**
	 * Get the result buckets for the job id starting with bucket id = 
	 * <code>startBucket</code> up to <code>endBucket</code>. One of either
	 * <code>startBucket</code> or <code>endBucket</code> should be non-zero else
	 * it is more efficient to use {@linkplain #buckets(String, boolean, int, int)}
	 * 
	 * @param jobId
	 * @param expand Include anomaly records
	 * @param skip Skip the first N Buckets. This parameter is for paging
	 * if not required set to 0.
	 * @param take Take only this number of Buckets
	 * @param startBucket The start bucket id. If 0 all buckets up to <code>endBucket</code>
	 * are returned
	 * @param endBucket The end bucket id. If 0 all buckets from <code>startBucket</code>
	 * are returned
	 * @return
	 */
	public Pagination<Map<String, Object>> buckets(String jobId, 
			boolean expand, int skip, int take,
			long startBucket, long endBucket)
	{
		RangeFilterBuilder fb = FilterBuilders.rangeFilter(Bucket.ID);
		if (startBucket > 0)
		{
			fb = fb.gte(startBucket);
		}
		if (endBucket > 0)
		{
			fb = fb.lt(endBucket);
		}

		return buckets(jobId, expand, skip, take, fb);
	}
	

	private Pagination<Map<String, Object>> buckets(String jobId, 
				boolean expand, int skip, int take,
				FilterBuilder fb)
	{	
				
		SortBuilder sb = new FieldSortBuilder(Bucket.ID)
								.ignoreUnmapped(true)
								.missing("_last")
								.order(SortOrder.ASC);
		
		SearchResponse searchResponse = m_Client.prepareSearch(jobId)
				.setTypes(Bucket.TYPE)		
				.addSort(sb)
				.setPostFilter(fb)
				.setFrom(skip).setSize(take)
				.get();
		
		List<Map<String, Object>> results = new ArrayList<>();
		for (SearchHit hit : searchResponse.getHits().getHits())
		{
			Map<String, Object> bucket  = hit.getSource();
			
			// TODO this is probably not the most efficient way to 
			// run the search. Try OR filters?
			if (expand)
			{
				Pagination<Map<String, Object>> page = this.records(
						jobId, hit.getId(), 0, DEFAULT_PAGE_SIZE);				
				bucket.put(Bucket.RECORDS, page.getDocuments());
			}

			results.add(bucket);
		}
		
		Pagination<Map<String, Object>> page = new Pagination<>();
		page.setDocuments(results);
		page.setHitCount(searchResponse.getHits().getTotalHits());
		page.setSkip(skip);
		page.setTake(take);
		page.setAllResults(searchResponse.getHits().getHits().length == page.getHitCount());
		
		return page;
	}
	
	
	/**
	 * Get the bucket by Id from the job. 
	 * Throws an ElasticSearchException if not found
	 * 
	 * @param jobId
	 * @param bucketId
	 * @param expand Include anomaly records
	 * @return
	 */
	public SingleDocument<Map<String, Object>> bucket(String jobId, String bucketId, 
										boolean expand)
	{
		GetResponse response = m_Client.prepareGet(jobId, Bucket.TYPE, bucketId).get();
				
		Map<String, Object> bucket = response.getSource();
		if (response.isExists() && expand)
		{
			Pagination<Map<String, Object>> page = this.records(jobId, 
					bucketId, 0, DEFAULT_PAGE_SIZE);
			bucket.put(Bucket.RECORDS, page.getDocuments());
		}
		
		SingleDocument<Map<String, Object>> doc = new SingleDocument<>();
		doc.setType(Bucket.TYPE);
		doc.setDocumentId(bucketId);
		if (response.isExists())
		{
			doc.setDocument(bucket);
		}
		
		return doc;
	}
	
	/**
	 * Get the detectors used in the job
	 * 
	 * @param jobId 
	 * @param bucketId
	 * @param skip Skip the first N Detectors. This parameter is for paging
	 * if not required set to 0.
	 * @param take Take only this number of Detectors
	 * @return
	 */
	public Pagination<Detector> detectors(String jobId, int skip,
			int take)
	{
		// get all detectors in job
		FilterBuilder fb = FilterBuilders.matchAllFilter();

		SearchResponse searchResponse = m_Client.prepareSearch(jobId)
				.setTypes(Detector.TYPE)		
				.setPostFilter(fb)
				.setFrom(skip).setSize(take)
				.get();

		List<Detector> results = new ArrayList<>();
		for (SearchHit hit : searchResponse.getHits().getHits())
		{
			Map<String, Object> m  = hit.getSource();
			results.add(new Detector(m));
		}
		
		Pagination<Detector> page = new Pagination<>();
		page.setDocuments(results);
		page.setHitCount(searchResponse.getHits().getTotalHits());
		page.setSkip(skip);
		page.setTake(take);
		page.setAllResults(searchResponse.getHits().getHits().length == page.getHitCount());
		
		return page;		
	}
	
	
	/**
	 * Get the anomaly records for the bucket generated by detector 
	 * 
	 * @param jobId
	 * @param bucketId 
	 * @param detectorName
	 * @param skip Skip the first N anomaly records. This parameter is for paging
	 * if not required set to 0.
	 * @param take Take only this number of anomaly records
	 * @return
	 */
	public Pagination<Map<String, Object>> records(String jobId, String bucketId,
			String detectorName, int skip, int take)
	{
		FilterBuilder bucketFilter= FilterBuilders.termFilter("_id", bucketId);
		FilterBuilder parentFilter = FilterBuilders.hasParentFilter(Bucket.TYPE, bucketFilter);
		FilterBuilder fb = FilterBuilders.termFilter(AnomalyRecord.DETECTOR_NAME, detectorName);
		FilterBuilder andFilter = FilterBuilders.andFilter(parentFilter, fb); 
		
		SortBuilder sb = new FieldSortBuilder(AnomalyRecord.ANOMALY_SCORE)
									.ignoreUnmapped(true)
									.missing("_last")
									.order(SortOrder.DESC);

		SearchResponse searchResponse = m_Client.prepareSearch(jobId)
				.setTypes(AnomalyRecord.TYPE)
				.setPostFilter(andFilter)
				.setFrom(skip).setSize(take)
				.addSort(sb)
				.get();

		List<Map<String, Object>> results = new ArrayList<>();
		for (SearchHit hit : searchResponse.getHits().getHits())
		{
			Map<String, Object> m  = hit.getSource();
			results.add(m);
		}
		
		Pagination<Map<String, Object>> page = new Pagination<>();
		page.setDocuments(results);
		page.setHitCount(searchResponse.getHits().getTotalHits());
		page.setSkip(skip);
		page.setTake(take);
		page.setAllResults(searchResponse.getHits().getHits().length == page.getHitCount());
		
		return page;	
	}
	
	/**
	 * Get all the anomaly records for the bucket for every detector 
	 * 
	 * @param jobId
	 * @param bucketId 
	 * @param skip Skip the first N Jobs. This parameter is for paging
	 * results if not required set to 0.
	 * @param take Take only this number of Jobs
	 * @return
	 */
	public Pagination<Map<String, Object>> records(String jobId, 
			String bucketId, int skip, int take)
	{
		FilterBuilder bucketFilter= FilterBuilders.termFilter("_id", bucketId);
		FilterBuilder parentFilter = FilterBuilders.hasParentFilter(Bucket.TYPE, bucketFilter);
				
		SortBuilder sb = new FieldSortBuilder(AnomalyRecord.ANOMALY_SCORE)
											.ignoreUnmapped(true)
											.missing("_last")
											.order(SortOrder.DESC);		
		
		SearchResponse searchResponse = m_Client.prepareSearch(jobId)
				.setTypes(AnomalyRecord.TYPE)
				.setPostFilter(parentFilter)
				.setFrom(skip).setSize(take)
				.addSort(sb)
				.get();

		List<Map<String, Object>> results = new ArrayList<>();
		for (SearchHit hit : searchResponse.getHits().getHits())
		{
			Map<String, Object> m  = hit.getSource();
			
			// TODO
			// This a hack to work round the deficiency in the 
			// Java API where source filtering hasn't been implemented.			
			m.remove(AnomalyRecord.DETECTOR_NAME);
			// TODO
			// remove the timestamp field that was added so the 
			// records can be sorted in Kibanna
			m.remove(Bucket.TIMESTAMP);
			
			results.add(m);
		}
		
		Pagination<Map<String, Object>> page = new Pagination<>();
		page.setDocuments(results);
		page.setHitCount(searchResponse.getHits().getTotalHits());
		page.setSkip(skip);
		page.setTake(take);
		page.setAllResults(searchResponse.getHits().getHits().length == page.getHitCount());
		
		return page;	
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
		getJobDetails(jobId);
		
		ProcessManager.ProcessStatus processStatus = m_ProcessManager.finishJob(jobId);	
		if (processStatus != ProcessManager.ProcessStatus.COMPLETED)
		{
			return false;
		}	
		
		return setJobFinishedTimeandStatus(jobId, new Date(), JobStatus.FINISHED);
	}
	
	
	@Override
	public boolean setJobFinishedTimeandStatus(String jobId, Date time, 
			JobStatus status)
	throws UnknownJobException
	{
		// update job status
		try
		{
			GetResponse response = m_Client.prepareGet(jobId, JobDetails.TYPE, 
					jobId).get();

			if (response.isExists() == false)
			{				
				s_Logger.error("Cannot finish job. No job found with jobId = " + jobId);
				return false;
			}

			long lastVersion = response.getVersion();

			JobDetails job = new JobDetails(response.getSource());
			job.setFinishedTime(new Date());
			job.setStatus(status);

			String content;
			try
			{
				content = jobToContent(job);
			}
			catch (IOException ioe)
			{
				s_Logger.error("Error serialising job");
				return false;
			}

			IndexResponse jobIndexResponse = m_Client.prepareIndex(
					jobId, JobDetails.TYPE, jobId)
					.setSource(content).get();

			if (jobIndexResponse.getVersion() <= lastVersion)
			{
				s_Logger.error("Error setting job to finished document not updated");
				return false;
			}
		}
		catch (IndexMissingException e)
		{
			String msg = String.format("Error writing the job '%s' finish time.", 
					jobId);
			s_Logger.error(msg);
			throw new UnknownJobException(jobId, msg, ErrorCodes.MISSING_JOB_ERROR);
		}
		
		return true;	
	}
	
	/**
	 * Stop the associated process and remove it from the Process
	 * Manager then delete all the job related documents from the 
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

		m_ProcessManager.finishJob(jobId);
		
		try 
		{
			DeleteIndexResponse response = m_Client.admin()
					.indices().delete(new DeleteIndexRequest(jobId)).get();
			
			return response.isAcknowledged();
		} 
		catch (InterruptedException|ExecutionException e) 
		{
			if (e.getCause() instanceof IndexMissingException)
			{
				String msg = String.format("No index with id '%s' in the database", jobId);
				s_Logger.warn(msg);
				throw new UnknownJobException(jobId, msg, 
						ErrorCodes.MISSING_JOB_ERROR);
			}
			else
			{
				String msg = "Error deleting index " + jobId;
				s_Logger.error(msg);
				throw new UnknownJobException(jobId, msg, 
						ErrorCodes.DATA_STORE_ERROR, e.getCause());
			}
		}
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
	 */
	public boolean dataToJob(String jobId, InputStream input) 
	throws UnknownJobException, NativeProcessRunException, MissingFieldException, 
		JsonParseException, JobInUseException 
	{
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
			
			setJobFinishedTimeandStatus(jobId, new Date(), JobStatus.FAILED);
			// rethrow
			throw ne;
		}
		finally 
		{
			updateLastDataTime(jobId, new Date()); 
		}
		
		return true;
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
		try
		{
			GetResponse response = m_Client.prepareGet(jobId, JobDetails.TYPE, 
					jobId).get();
			
			if (response.isExists() == false)
			{				
				s_Logger.error("Cannot update last data time- no job found with jobId = " + jobId);
				return false;
			}

			long lastVersion = response.getVersion();

			JobDetails job = new JobDetails(response.getSource());
			job.setLastDataTime(new Date());
			job.setStatus(JobStatus.RUNNING);

			String content;
			try
			{
				content = jobToContent(job);
			}
			catch (JsonProcessingException e)
			{
				s_Logger.error("Error serialising job cannot update time of "
						+ "last data", e);
				return false;
			}

			IndexResponse jobIndexResponse = m_Client.prepareIndex(
					jobId, JobDetails.TYPE, jobId)
					.setSource(content).get();

			if (jobIndexResponse.getVersion() <= lastVersion)
			{
				s_Logger.error("Error setting job last data time document not updated");
				return false;
			}
			
			return true;
		}
		catch (IndexMissingException e)
		{
			String msg = String.format("Error writing the job '%s' last data time.", jobId);
			throw new UnknownJobException(jobId, msg, ErrorCodes.MISSING_JOB_ERROR);
		}
	}
	
	
	@Override
	public DetectorState getPersistedState(String jobId)
	throws UnknownJobException 
	{
		ElasticSearchPersister es = new ElasticSearchPersister(jobId, m_Node.client());
		return es.retrieveDetectorState();
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
		String id = String.format("%s-%05d", m_DateFormat.format(new Date()),
						m_IdSequence.incrementAndGet());		
		return id;
	}		
		
	/**
	 * Stops the ElasticSearch client and the Process Manager
	 */
	public void stop()
	{
		m_ProcessManager.stop();
		m_Node.close();
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
			GetResponse res = m_Client.prepareGet(refId, JobDetails.TYPE, refId).get();
			if (res.isExists())
			{
				return new JobDetails(res.getSourceAsMap());
			}
			else
			{
				throw new UnknownJobException(refId, "Cannot find "
					+ "referenced job with id '" + refId + "'", ErrorCodes.UNKNOWN_JOB_REFERENCE);
			}
		}
		catch (IndexMissingException e)
		{
			throw new UnknownJobException(refId, "Missing index: Cannot find "
					+ "referenced job with id '" + refId + "'", ErrorCodes.UNKNOWN_JOB_REFERENCE);
	
		}
	}
	
	private String jobToContent(JobDetails job)
	throws JsonProcessingException
	{
		String json = m_ObjectMapper.writeValueAsString(job);
		return json;
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
	 * Attempt to get usage info from the C++ process, add extra fields and
	 * persist to ElasticSearch.  Any failures are logged but do not otherwise
	 * impact operation of this process.
	 */
	private void saveUsageInfo()
	{
		// This will be a JSON document in string form
		String backendUsageInfo = m_ProcessManager.getUsageInfo();

		// Try to parse the string returned from the C++ process and add the
		// extra fields
		ObjectNode doc;
		try
		{
			doc = (ObjectNode)m_ObjectMapper.readTree(backendUsageInfo);
		}
		catch (IOException e)
		{
			s_Logger.warn("Failed to parse JSON document " + backendUsageInfo, e);
			return;
		}
		catch (ClassCastException e)
		{
			s_Logger.warn("Parsed non-object JSON document " + backendUsageInfo, e);
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

		// Try to persist the modified document to ElasticSearch
		try
		{
			m_Client.prepareIndex(USAGE_INFO_INDEX, USAGE_INFO_TYPE, USAGE_INFO_ID)
					.setSource(doc.toString())
					.execute().actionGet();
		}
		catch (Exception e)
		{
			s_Logger.warn("Error writing Prelert info to ElasticSearch", e);
			return;
		}

		s_Logger.info("Wrote Prelert info " + doc.toString() + " to ElasticSearch");
	}
}
