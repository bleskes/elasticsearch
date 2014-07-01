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
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
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
import com.fasterxml.jackson.databind.JsonNode;
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
import com.prelert.job.warnings.HighProportionOfBadTimestampsException;
import com.prelert.job.warnings.OutOfOrderRecordsException;
import com.prelert.job.warnings.elasticsearch.ElasticSearchStatusReporterFactory;
import com.prelert.job.DetectorState;
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
import com.prelert.rs.data.Detector;
import com.prelert.rs.data.ErrorCode;
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
	static public final String PRELERT_INFO_INDEX = "prelert-int";
	static public final String PRELERT_INFO_TYPE = "info";
	static public final String PRELERT_INFO_ID = "infoStats";

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

	/**
	 * ElasticSearch settings that instruct the node not to accept HTTP, not to
	 * attempt multicast discovery and to only look for another node to connect
	 * to on the local machine.
	 */
	static public final Settings LOCAL_SETTINGS;
	static
	{
		LOCAL_SETTINGS = ImmutableSettings.settingsBuilder()
				.put("http.enabled", "false")
				.put("discovery.zen.ping.multicast.enabled", "false")
				.put("discovery.zen.ping.unicast.hosts", "localhost")
				.build();
	}

	private Node m_Node;
	private Client m_Client;
	
	private ProcessManager m_ProcessManager;
	
	private AtomicLong m_IdSequence;	
	private DateFormat m_JobIdDateFormat;
	
	private ObjectMapper m_ObjectMapper;

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
	 * Create a JobManager and a default Elasticsearch node
	 * on localhost with the default port.
	 * 
	 * @param elasticSearchClusterName The name of the ElasticSearch cluster
	 */
	public JobManager(String elasticSearchClusterName)
	{
		// Multicast discovery is expected to be disabled on the ElasticSearch
		// data node, so disable it for this embedded node too and tell it to
		// expect the data node to be on the same machine
		this(nodeBuilder().settings(LOCAL_SETTINGS).client(true).clusterName(elasticSearchClusterName).node());

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
				new ElasticSearchResultsReaderFactory(m_Node),
				new ElasticSearchStatusReporterFactory(m_Node));
		
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
				return m_ObjectMapper.convertValue(response.getSource(), JobDetails.class);
			}		
			else 
			{
				String msg = "No details for job with id " + jobId;
				s_Logger.warn(msg);
				throw new UnknownJobException(jobId, msg,
						ErrorCode.MISSING_JOB_ERROR);
			}
		}
		catch (IndexMissingException e)
		{
			// the job does not exist
			String msg = "Missing Index no job with id " + jobId;
			s_Logger.warn(msg);
			throw new UnknownJobException(jobId, "No known job with id '" + jobId + "'", 
					ErrorCode.MISSING_JOB_ERROR);
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
	public Pagination<JobDetails> getJobs(int skip, int take)
	{
		FilterBuilder fb = FilterBuilders.matchAllFilter();
		SortBuilder sb = new FieldSortBuilder(JobDetails.ID)
							.ignoreUnmapped(true)
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
			jobs.add(m_ObjectMapper.convertValue(hit.getSource(), JobDetails.class)); 
		}
		
		Pagination<JobDetails> page = new Pagination<>();
		page.setDocuments(jobs);
		page.setHitCount(response.getHits().getTotalHits());
		page.setSkip(skip);
		page.setTake(take);
		
		return page;
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
			jobIdIsUnigue(jobId);
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
					
		try		
		{
			XContentBuilder jobMapping = ElasticSearchMappings.jobMapping();
			XContentBuilder bucketMapping = ElasticSearchMappings.bucketMapping();
			XContentBuilder detectorMapping = ElasticSearchMappings.detectorMapping();
			XContentBuilder recordMapping = ElasticSearchMappings.recordMapping();
			XContentBuilder detectorStateMapping = ElasticSearchMappings.detectorStateMapping();
			
			
			CreateIndexRequestBuilder indexBuilder = m_Client.admin().indices()
								.prepareCreate(jobId)					
								.addMapping(JobDetails.TYPE, jobMapping)
								.addMapping(Bucket.TYPE, bucketMapping)
								.addMapping(Detector.TYPE, detectorMapping)
								.addMapping(AnomalyRecord.TYPE, recordMapping)
								.addMapping(DetectorState.TYPE, detectorStateMapping);
			
			long lasttime = System.currentTimeMillis();
			indexBuilder.get();
			long now = System.currentTimeMillis();
			
			String json = m_ObjectMapper.writeValueAsString(jobDetails);
			
			System.out.println("Index create = " + (now - lasttime) + "ms");
			lasttime = now;
			
			m_Client.prepareIndex(jobId, JobDetails.TYPE, jobId)
					.setSource(json)
					.get();
					
			now = System.currentTimeMillis();
			System.out.println("Index put = " + (now - lasttime) + "ms");
			lasttime = now;
			
			// wait for the job to be indexed in ElasticSearch			
			m_Client.admin().indices().refresh(new RefreshRequest(jobId)).actionGet();
			
			now = System.currentTimeMillis();
			System.out.println("Refresh = " + (now - lasttime) + "ms");
			lasttime = now;
			
			return jobDetails;
		}
		catch (IOException e)
		{
			s_Logger.error("Error writing ElasticSearch mappings", e);
			throw e;
		}
	}

	
	/**
	 * Set the job's description.
	 * If the description cannot be set an exception is thrown.
	 * 
	 * @param jobId
	 * @param description
	 * @throws UnknownJobException
	 * @throws JsonProcessingException 
	 */
	public void setDescription(String jobId, String description)
	throws UnknownJobException, JsonProcessingException
	{
		JobDetails job = getJobDetails(jobId); 
		job.setDescription(description);
		
		String content = jobToContent(job);
		m_Client.prepareIndex(jobId, JobDetails.TYPE, jobId)
				.setSource(content).get();
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
			
			// Remove the Kibana/Logstash '@timestamp' entry as stored in Elasticsearch, 
			// and replace using the API 'timestamp' key.
			Object timestamp = bucket.remove(ElasticSearchMappings.ES_TIMESTAMP);
			bucket.put(Bucket.TIMESTAMP, timestamp);
			
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
					
		SingleDocument<Map<String, Object>> doc = new SingleDocument<>();
		doc.setType(Bucket.TYPE);
		doc.setDocumentId(bucketId);
		if (response.isExists())
		{
			Map<String, Object> bucket = response.getSource();

			// Remove the Kibana/Logstash '@timestamp' entry as stored in Elasticsearch, 
			// and replace using the API 'timestamp' key.
			Object timestamp = bucket.remove(ElasticSearchMappings.ES_TIMESTAMP);
			bucket.put(Bucket.TIMESTAMP, timestamp);
			
			if (response.isExists() && expand)
			{
				Pagination<Map<String, Object>> page = this.records(jobId, 
						bucketId, 0, DEFAULT_PAGE_SIZE);
				bucket.put(Bucket.RECORDS, page.getDocuments());
			}
			
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
			
			// TODO
			// This a hack to work round the deficiency in the 
			// Java API where source filtering hasn't been implemented.			
			m.remove(AnomalyRecord.DETECTOR_NAME);
			// TODO
			// remove the timestamp field that was added so the 
			// records can be sorted in Kibanna
			m.remove(ElasticSearchMappings.ES_TIMESTAMP);
			
			results.add(m);
			
			// TODO
			// remove the timestamp field that was added so the 
			// records can be sorted in Kibanna
			m.remove(Bucket.TIMESTAMP);
		}
		
		Pagination<Map<String, Object>> page = new Pagination<>();
		page.setDocuments(results);
		page.setHitCount(searchResponse.getHits().getTotalHits());
		page.setSkip(skip);
		page.setTake(take);
		
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
			m.remove(ElasticSearchMappings.ES_TIMESTAMP);
			
			results.add(m);
		}
		
		Pagination<Map<String, Object>> page = new Pagination<>();
		page.setDocuments(results);
		page.setHitCount(searchResponse.getHits().getTotalHits());
		page.setSkip(skip);
		page.setTake(take);
		
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
		return (processStatus == ProcessManager.ProcessStatus.COMPLETED);
	}
	
	@Override
	public boolean setJobStatus(String jobId, JobStatus status)
	throws UnknownJobException
	{
		// update job status
		try
		{
			GetResponse response = m_Client.prepareGet(jobId, JobDetails.TYPE, 
					jobId).get();

			if (response.isExists() == false)
			{				
				s_Logger.error("Cannot set job status no job found with jobId = " 
							+ jobId);
				return false;
			}

			long lastVersion = response.getVersion();

			JobDetails job = m_ObjectMapper.convertValue(response.getSource(), 
					JobDetails.class);
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
				s_Logger.error("Error saving job- document not updated");
				return false;
			}

			m_Client.admin().indices().refresh(new RefreshRequest(jobId)).actionGet();
		}
		catch (IndexMissingException e)
		{
			String msg = String.format("Unknown job '%s'. Error setting the job's status.", 
					jobId);
			s_Logger.error(msg);
			throw new UnknownJobException(jobId, msg, ErrorCode.MISSING_JOB_ERROR);
		}
		
		return true;		
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
				s_Logger.error("Cannot set job finish time and status no job "
						+ "found with jobId = " + jobId);
				return false;
			}

			long lastVersion = response.getVersion();

			JobDetails job = m_ObjectMapper.convertValue(response.getSource(), 
					JobDetails.class);
			job.setFinishedTime(new Date());
			job.setStatus(status);

			String content;
			try
			{
				content = jobToContent(job);
			}
			catch (IOException ioe)
			{
				s_Logger.error("Error serialising job details");
				return false;
			}

			IndexResponse jobIndexResponse = m_Client.prepareIndex(
					jobId, JobDetails.TYPE, jobId)
					.setSource(content).get();

			if (jobIndexResponse.getVersion() <= lastVersion)
			{
				s_Logger.error("Error saving job- document not updated");
				return false;
			}

			m_Client.admin().indices().refresh(new RefreshRequest(jobId)).actionGet();
		}
		catch (IndexMissingException e)
		{
			String msg = String.format("Unknown job '%s'. Error writing the job's "
					+ "finish time and status.", jobId);
			s_Logger.error(msg);
			throw new UnknownJobException(jobId, msg, ErrorCode.MISSING_JOB_ERROR);
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
						ErrorCode.MISSING_JOB_ERROR);
			}
			else
			{
				String msg = "Error deleting index " + jobId;
				s_Logger.error(msg);
				throw new UnknownJobException(jobId, msg, 
						ErrorCode.DATA_STORE_ERROR, e.getCause());
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
			getJobDetails(jobId).getStatus().isRunning() == false &&
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

			JobDetails job = m_ObjectMapper.convertValue(response.getSource(), 
					JobDetails.class);
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
			String msg = String.format("Unknown job '%s'. Error writing the job's last data time.", jobId);
			throw new UnknownJobException(jobId, msg, ErrorCode.MISSING_JOB_ERROR);
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
			GetResponse response = m_Client.prepareGet(refId, JobDetails.TYPE, refId).get();
			if (response.isExists())
			{
				return m_ObjectMapper.convertValue(response.getSource(), JobDetails.class);
			}
			else
			{
				throw new UnknownJobException(refId, "Cannot find "
					+ "referenced job with id '" + refId + "'", ErrorCode.UNKNOWN_JOB_REFERENCE);
			}
		}
		catch (IndexMissingException e)
		{
			throw new UnknownJobException(refId, "Missing index: Cannot find "
					+ "referenced job with id '" + refId + "'", ErrorCode.UNKNOWN_JOB_REFERENCE);
	
		}
	}
	
	
	/**
	 * Return true if the job id is unique else if it is already used
	 * throw JobAliasAlreadyExistsException
	 * @param jobId 
	 * @return
	 * @throws JobIdAlreadyExistsException
	 */
	private boolean jobIdIsUnigue(String jobId)
	throws JobIdAlreadyExistsException
	{
		IndicesExistsResponse res = 
				m_Client.admin().indices().exists(new IndicesExistsRequest(jobId)).actionGet();
		
		if (res.isExists())
		{
			throw new JobIdAlreadyExistsException(jobId);
		}
		
		return true;
	}
	
	/**
	 * Convert job to a JSON string using the object mapper.
	 * 
	 * @param job
	 * @return
	 * @throws JsonProcessingException
	 */
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

		// Try to persist the modified document to ElasticSearch
		try
		{
			m_Client.prepareIndex(PRELERT_INFO_INDEX, PRELERT_INFO_TYPE, PRELERT_INFO_ID)
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
