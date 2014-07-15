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

package com.prelert.job.persistence.elasticsearch;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.prelert.job.DetectorState;
import com.prelert.job.JobDetails;
import com.prelert.job.JobIdAlreadyExistsException;
import com.prelert.job.JobStatus;
import com.prelert.job.UnknownJobException;
import com.prelert.job.persistence.JobProvider;
import com.prelert.job.usage.Usage;
import com.prelert.rs.data.AnomalyRecord;
import com.prelert.rs.data.Bucket;
import com.prelert.rs.data.Detector;
import com.prelert.rs.data.ErrorCode;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;

public class ElasticSearchJobProvider implements JobProvider
{
	static public final Logger s_Logger = Logger.getLogger(ElasticSearchJobProvider.class);
			
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
	
	/**
	 * The index to store total usage/metering information
	 */
	static public final String PRELERT_USAGE_INDEX = "prelert-usage";

	/**
	 * Where to store the prelert info in ElasticSearch - must match what's
	 * expected by kibana/engineAPI/app/directives/prelertLogUsage.js
	 */
	static public final String PRELERT_INFO_INDEX = "prelert-int";
	static public final String PRELERT_INFO_TYPE = "info";
	static public final String PRELERT_INFO_ID = "infoStats";
	
	static public final int DEFAULT_PAGE_SIZE = 100;
	
	
	private Node m_Node;
	private Client m_Client;
	
	private ObjectMapper m_ObjectMapper;
	
	private int m_PageSize;
	
	public ElasticSearchJobProvider(String elasticSearchClusterName)
	{
		// Multicast discovery is expected to be disabled on the ElasticSearch
		// data node, so disable it for this embedded node too and tell it to
		// expect the data node to be on the same machine
		this(nodeBuilder().settings(LOCAL_SETTINGS).client(true)
				.clusterName(elasticSearchClusterName).node());

		s_Logger.info("Connecting to ElasticSearch cluster '"
				+ elasticSearchClusterName + "'");		
	}
	
	public ElasticSearchJobProvider(Node node)
	{
		m_Node = node;
		m_Client = m_Node.client();
		
		m_ObjectMapper = new ObjectMapper();
		m_ObjectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		
		createUsageMeteringIndex();
		
		m_PageSize = DEFAULT_PAGE_SIZE;
	}
	
	/**
	 * Close the Elasticsearch node
	 */
	@Override
	public void close() throws IOException 
	{
		m_Node.close();
	}

	
	public Client getClient()
	{
		return m_Client;
	}
	
	public Node getNode()
	{
		return m_Node;
	}
	
	public void setPageSize(int pageSize)
	{
		m_PageSize = pageSize;
	}
	
	/**
	 * If the {@value ElasticSearchJobProvider#PRELERT_USAGE_INDEX} index does not exist
	 * create it here with the usage document mapping.
	 */
	private void createUsageMeteringIndex()
	{
		try 
		{
			boolean indexExists = m_Client.admin().indices()
					.exists(new IndicesExistsRequest(PRELERT_USAGE_INDEX))
					.get().isExists();

			if (indexExists == false)
			{
				s_Logger.info("Creating the internal '" + PRELERT_USAGE_INDEX + "' index");

				XContentBuilder usageMapping = ElasticSearchMappings.usageMapping();

				m_Client.admin().indices().prepareCreate(PRELERT_USAGE_INDEX)					
								.addMapping(Usage.TYPE, usageMapping)
								.get();
			}
		} 
		catch (InterruptedException | ExecutionException e) 
		{
			s_Logger.warn("Error checking the usage metering index", e);
		}
		catch (IOException e) 
		{
			s_Logger.warn("Error creating the usage metering index", e);
		}
		
	}

	@Override
	public boolean jobExists(String jobId) throws UnknownJobException 
	{
		try
		{
			GetResponse response = m_Client.prepareGet(jobId, JobDetails.TYPE, jobId)
							.setFetchSource(false)
							.setFields()				
							.get();
			
			if (response.isExists() == false)
			{
				String msg = "No job document with id " + jobId;
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
		
		return true;
	}
	

	@Override
	public boolean jobIdIsUnique(String jobId) throws JobIdAlreadyExistsException 
	{
		IndicesExistsResponse res = 
				m_Client.admin().indices().exists(new IndicesExistsRequest(jobId)).actionGet();
		
		if (res.isExists())
		{
			throw new JobIdAlreadyExistsException(jobId);
		}
		
		return true;
	}

	
	@Override
	public JobDetails getJobDetails(String jobId) throws UnknownJobException 
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

	@Override
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
	 * Create the Elasticsearch index and the mappings
	 * @throws  
	 */
	@Override
	public boolean createJob(JobDetails job) 
	throws JobIdAlreadyExistsException 
	{
		try		
		{
			XContentBuilder jobMapping = ElasticSearchMappings.jobMapping();
			XContentBuilder bucketMapping = ElasticSearchMappings.bucketMapping();
			XContentBuilder detectorMapping = ElasticSearchMappings.detectorMapping();
			XContentBuilder recordMapping = ElasticSearchMappings.recordMapping();
			XContentBuilder detectorStateMapping = ElasticSearchMappings.detectorStateMapping();
			XContentBuilder usageMapping = ElasticSearchMappings.usageMapping();
			
			m_Client.admin().indices()
					.prepareCreate(job.getId())					
					.addMapping(JobDetails.TYPE, jobMapping)
					.addMapping(Bucket.TYPE, bucketMapping)
					.addMapping(Detector.TYPE, detectorMapping)
					.addMapping(AnomalyRecord.TYPE, recordMapping)
					.addMapping(DetectorState.TYPE, detectorStateMapping)
					.addMapping(Usage.TYPE, usageMapping)
					.get();

			
			String json = m_ObjectMapper.writeValueAsString(job);
						
			m_Client.prepareIndex(job.getId(), JobDetails.TYPE, job.getId())
					.setSource(json)
					.setRefresh(true)
					.get();
			
			return true;
		}
		catch (ElasticsearchException e)
		{
			s_Logger.error("Error writing ElasticSearch mappings", e);
			throw e;
		} 
		catch (IOException e) 
		{
			s_Logger.error("Error writing ElasticSearch mappings", e);
		}
		
		return false;
	}

	@Override
	public boolean updateJob(String jobId, Map<String, Object> updates)
	throws UnknownJobException 
	{
		if (jobExists(jobId))
		{
			m_Client.prepareUpdate(jobId, JobDetails.TYPE, jobId)
									.setDoc(updates)
									.setRefresh(true)
									.get();
			
			return true;
		}
		
		return false;
	}
	
	@Override
	public boolean setJobStatus(String jobId, JobStatus status)
	throws UnknownJobException
	{
		Map<String, Object> update = new HashMap<>();
		update.put(JobDetails.STATUS, status);
		return this.updateJob(jobId, update);
	
	}

	@Override
	public boolean setJobFinishedTimeandStatus(String jobId, Date time, 
			JobStatus status)
	throws UnknownJobException
	{
		Map<String, Object> update = new HashMap<>();
		update.put(JobDetails.FINISHED_TIME, time);
		update.put(JobDetails.STATUS, status);
		return this.updateJob(jobId, update);	
	}
	
	
	@Override
	public boolean deleteJob(String jobId) throws UnknownJobException 
	{
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
				String msg = String.format("Cannot delete job - no index with id '%s' in the database", jobId);
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

	/* Results */
	@Override
	public Pagination<Map<String, Object>> buckets(String jobId,
			boolean expand, int skip, int take) 
	{
		FilterBuilder fb = FilterBuilders.matchAllFilter();
		
		return buckets(jobId, expand, skip, take, fb);
	}

	@Override
	public Pagination<Map<String, Object>> buckets(String jobId,
			boolean expand, int skip, int take, long startBucket, long endBucket) 
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
						jobId, hit.getId(), 0, m_PageSize);				
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
	
	public SingleDocument<Map<String, Object>> bucket(String jobId, 
			String bucketId, boolean expand)
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
						bucketId, 0, m_PageSize);
				bucket.put(Bucket.RECORDS, page.getDocuments());
			}
			
			doc.setDocument(bucket);
		}
		
		return doc;
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
		FilterBuilder bucketFilter;
		
		if (bucketId.equals("_all"))
		{
			bucketFilter = FilterBuilders.matchAllFilter();
		}
		else
		{
			bucketFilter = FilterBuilders.termFilter("_id", bucketId);
		}
		
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
	
	
	public List<TimeScore> getRawScores(String jobId)
	{
		FilterBuilder fb = FilterBuilders.matchAllFilter();
		
		SortBuilder sb = new FieldSortBuilder(Bucket.ID)
								.order(SortOrder.DESC);	
		
		SearchResponse searchResponse = m_Client.prepareSearch(jobId)
				.setTypes(Bucket.TYPE)
				.setFetchSource(false)
				.addField(Bucket.ID)
				.addField(Bucket.ANOMALY_SCORE)
				.setPostFilter(fb)
				.setFrom(0).setSize(500)
				.addSort(sb)
				.get();
		
		List<TimeScore> result = new ArrayList<>();
		for (SearchHit hit : searchResponse.getHits().getHits())
		{
			TimeScore ts = new TimeScore(hit.field(Bucket.ID).<String>value(), 
								hit.field(Bucket.ANOMALY_SCORE).value().toString());
			result.add(ts);
		}
		
		return result;
	}

	/**
	 * Always returns true
	 */
	@Override
	public boolean savePrelertInfo(String infoDoc) 
	{
		m_Client.prepareIndex(PRELERT_INFO_INDEX, PRELERT_INFO_TYPE, PRELERT_INFO_ID)
						.setSource(infoDoc)
						.execute().actionGet();
		
		return true;
	}

	@Override
	public DetectorState getDetectorState(String jobId) 
	throws UnknownJobException
	{
		DetectorState detectorState = new DetectorState();
		
		FilterBuilder fb = FilterBuilders.matchAllFilter();
		
		SearchRequestBuilder searchBuilder = m_Client.prepareSearch(jobId)
				.setTypes(DetectorState.TYPE)		
				.setPostFilter(fb);
			
		try
		{
			final int PAGE_SIZE = 50;		
			int from=0, size=PAGE_SIZE;
			boolean getNextPage = true;
			while (getNextPage)
			{
				searchBuilder.setFrom(from).setSize(size); 
				SearchResponse searchResponse = searchBuilder.get();

				for (SearchHit hit : searchResponse.getHits().getHits())
				{
					String detectorName = hit.getSource().get(DetectorState.DETECTOR_NAME).toString();
					String state = hit.getSource().get(DetectorState.SERIALISED_MODEL).toString();
					detectorState.setDetectorState(detectorName, state);
				}		

				if (searchResponse.getHits().getTotalHits() <= (from + size))
				{
					getNextPage = false;
				}
				else
				{
					from += size;
					size += PAGE_SIZE;
				}
			}
		}
		catch (IndexMissingException e)
		{
			s_Logger.error("Unknown job '" + jobId + "'. Cannot read persisted state");
			throw new UnknownJobException(jobId, 
					"Cannot read persisted state", ErrorCode.MISSING_DETECTOR_STATE);
		}
		return detectorState;
	}





}
