/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Inc 2006-2014     *
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

package com.prelert.job;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
import org.elasticsearch.search.sort.SortOrder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.prelert.job.persistence.elasticsearch.ElasticSearchMappings;
import com.prelert.job.persistence.elasticsearch.ElasticSearchPersister;
import com.prelert.job.persistence.elasticsearch.ElasticSearchResultsReaderFactory;
import com.prelert.job.process.JobDetailsProvider;
import com.prelert.job.process.ProcessManager;
import com.prelert.job.process.ProcessManager.ProcessStatus;
import com.prelert.job.NativeProcessRunException;
import com.prelert.rs.data.AnomalyRecord;
import com.prelert.rs.data.Bucket;
import com.prelert.rs.data.Detector;
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
		doc.setId(jobId);

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
				throw new UnknownJobException(jobId, "No job details for job");
			}
		}
		catch (IndexMissingException e)
		{
			// the job does not exist
			String msg = "Missing Index: No job with id " + jobId;
			s_Logger.warn(msg);
			throw new UnknownJobException(jobId, msg);
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
		SearchResponse response = m_Client.prepareSearch("_all")
				.setTypes(JobDetails.TYPE)
				.setPostFilter(fb)
				.setFrom(skip).setSize(take)
				.addSort(JobDetails.ID, SortOrder.DESC)  
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
	throws UnknownJobException, JsonProcessingException
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
		catch (JsonProcessingException e)
		{
			s_Logger.error(e);
			throw e;
		}
		catch (IOException e)
		{
			s_Logger.error(e);
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
				
		SearchResponse searchResponse = m_Client.prepareSearch(jobId)
				.setTypes(Bucket.TYPE)		
				.addSort(Bucket.ID, SortOrder.ASC)
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
		doc.setId(bucketId);
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

		SearchResponse searchResponse = m_Client.prepareSearch(jobId)
				.setTypes(AnomalyRecord.TYPE)
				.setPostFilter(andFilter)
				.setFrom(skip).setSize(take)
				.addSort(AnomalyRecord.ANOMALY_SCORE, SortOrder.DESC)
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

		SearchResponse searchResponse = m_Client.prepareSearch(jobId)
				.setTypes(AnomalyRecord.TYPE)
				.setPostFilter(parentFilter)
				.setFrom(skip).setSize(take)
				.addSort(AnomalyRecord.ANOMALY_SCORE, SortOrder.DESC)
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
	 * Stop the running job and mark it as finished.<br/>
	 * 
	 * @param jobId The job to stop
	 * @return true if the job stopped successfully
	 * @throws UnknownJobException 
	 * @throws NativeProcessRunException 
	 */
	public boolean finishJob(String jobId) 
	throws UnknownJobException, NativeProcessRunException
	{
		s_Logger.debug("Finish job " + jobId);
		ProcessManager.ProcessStatus processStatus = m_ProcessManager.finishJob(jobId);			
		if (processStatus != ProcessManager.ProcessStatus.COMPLETED)
		{
			return false;
		}	
		
		return setJobFinishedTimeandStatus(jobId, new Date(), JobStatus.FINISHED);
	}
	
	
	private boolean setJobFinishedTimeandStatus(String jobId, Date time, 
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
			throw new UnknownJobException(jobId, "Error writing the job's finish time.");
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
	 */
	public boolean deleteJob(String jobId)
	throws UnknownJobException, NativeProcessRunException
	{		
		s_Logger.debug("Deleting job '" + jobId + "'");
		
		try
		{
			ProcessStatus stopStatus = m_ProcessManager.finishJob(jobId);
			if (stopStatus == ProcessStatus.IN_USE)
			{
				s_Logger.error("Cannot delete job as the process is in use");
				return false;
			}
		}
		catch (UnknownJobException e)
		{
			// if the job is already finished then catch this
		}
		
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
				String msg = "Cannot delete job";
				s_Logger.warn(msg);
				throw new UnknownJobException(jobId, msg);
			}
			else
			{
				String msg = "Error deleting index " + jobId;
				s_Logger.error(msg, e);
				throw new NativeProcessRunException(msg, e);
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
	 */
	public boolean dataToJob(String jobId, InputStream input) 
	throws UnknownJobException, NativeProcessRunException
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
			catch (UnknownJobException | NativeProcessRunException e)
			{
				s_Logger.warn("Error finished job after dataToJob failed", e);
			}
			
			setJobFinishedTimeandStatus(jobId, new Date(), JobStatus.FAILED);
			// rethrow
			throw ne;
		}
		
		return updateLastDataTime(jobId, new Date()); // time now
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
				s_Logger.error("Cannot finish job. No job found with jobId = " + jobId);
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
			catch (IOException ioe)
			{
				s_Logger.error("Error serialising job cannot update time of "
						+ "last data");
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
			throw new UnknownJobException(jobId, "Error writing the job's last data time.");
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
	 * with zeros.</br>
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
		//return "testjob";
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
				throw new UnknownJobException(refId, "Cannot create job from "
					+ "referenced job id. Job with id '" + refId +"' does not exist");
			}
		}
		catch (IndexMissingException e)
		{
			throw new UnknownJobException(refId, "Cannot create job from "
					+ "referenced job. Job with id '" + refId +"' does not exist");			
		}
	}
	
	private String jobToContent(JobDetails job)
	throws IOException
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

}