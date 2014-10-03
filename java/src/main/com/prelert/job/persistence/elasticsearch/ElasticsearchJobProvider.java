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
import java.util.Arrays;
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
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetRequestBuilder;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import org.elasticsearch.index.get.GetField;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.IdsFilterBuilder;
import org.elasticsearch.index.query.RangeFilterBuilder;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.node.Node;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortOrder;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.prelert.job.DetectorState;
import com.prelert.job.JobDetails;
import com.prelert.job.JobIdAlreadyExistsException;
import com.prelert.job.JobStatus;
import com.prelert.job.UnknownJobException;
import com.prelert.job.persistence.JobProvider;
import com.prelert.job.quantiles.Quantiles;
import com.prelert.job.quantiles.QuantilesState;
import com.prelert.job.usage.Usage;
import com.prelert.rs.data.AnomalyRecord;
import com.prelert.rs.data.Bucket;
import com.prelert.rs.data.Detector;
import com.prelert.rs.data.ErrorCode;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;

public class ElasticsearchJobProvider implements JobProvider
{
	static public final Logger s_Logger = Logger.getLogger(ElasticsearchJobProvider.class);
			
	/**
	 * Elasticsearch settings that instruct the node not to accept HTTP, not to
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
	 * Where to store the prelert info in Elasticsearch - must match what's
	 * expected by kibana/engineAPI/app/directives/prelertLogUsage.js
	 */
	static public final String PRELERT_INFO_INDEX = "prelert-int";
	static public final String PRELERT_INFO_TYPE = "info";
	static public final String PRELERT_INFO_ID = "infoStats";
	
	static public final String _PARENT = "_parent";
	
	static private final List<String> SECONDARY_SORT = new ArrayList<>(); 
//	static 
//	{
//		SECONDARY_SORT.add(AnomalyRecord.ANOMALY_SCORE);
//	}
	
	
	private Node m_Node;
	private Client m_Client;
	
	private ObjectMapper m_ObjectMapper;
	

	public ElasticsearchJobProvider(String elasticSearchClusterName)
	{
		// Multicast discovery is expected to be disabled on the Elasticsearch
		// data node, so disable it for this embedded node too and tell it to
		// expect the data node to be on the same machine
		this(nodeBuilder().settings(LOCAL_SETTINGS).client(true)
				.clusterName(elasticSearchClusterName).node());

		s_Logger.info("Connecting to Elasticsearch cluster '"
				+ elasticSearchClusterName + "'");		
	}
	
	public ElasticsearchJobProvider(Node node)
	{
		m_Node = node;
		m_Client = m_Node.client();
		
		m_ObjectMapper = new ObjectMapper();
		m_ObjectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		m_ObjectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		
		createUsageMeteringIndex();
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
		
	/**
	 * If the {@value ElasticsearchJobProvider#PRELERT_USAGE_INDEX} index does 
	 * not exist create it here with the usage document mapping.
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

				XContentBuilder usageMapping = ElasticsearchMappings.usageMapping();

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
								.order(SortOrder.ASC);

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
			XContentBuilder jobMapping = ElasticsearchMappings.jobMapping();
			XContentBuilder bucketMapping = ElasticsearchMappings.bucketMapping();
			XContentBuilder detectorMapping = ElasticsearchMappings.detectorMapping();
			XContentBuilder recordMapping = ElasticsearchMappings.recordMapping();
			XContentBuilder quantilesMapping = ElasticsearchMappings.quantilesMapping();
			XContentBuilder detectorStateMapping = ElasticsearchMappings.detectorStateMapping();
			XContentBuilder usageMapping = ElasticsearchMappings.usageMapping();
						
			m_Client.admin().indices()
					.prepareCreate(job.getId())					
					.addMapping(JobDetails.TYPE, jobMapping)
					.addMapping(Bucket.TYPE, bucketMapping)
					.addMapping(Detector.TYPE, detectorMapping)
					.addMapping(AnomalyRecord.TYPE, recordMapping)
					.addMapping(Quantiles.TYPE, quantilesMapping)
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
			s_Logger.error("Error writing Elasticsearch mappings", e);
			throw e;
		} 
		catch (IOException e) 
		{
			s_Logger.error("Error writing Elasticsearch mappings", e);
		}
		
		return false;
	}
	
	/**
	 * Returns null if the field cannot be found or converted to 
	 * type V
	 */
	@Override
	public <V> V getField(String jobId, String fieldName) 
	{
		try
		{
			GetResponse response = m_Client
					.prepareGet(jobId, JobDetails.TYPE, jobId)
					.setFields(fieldName)
					.get();
			try 
			{
				GetField f = response.getField(fieldName);
				if (f != null)
				return (f != null) ? (V)f.getValue() : null;
			}
			catch (ClassCastException e)
			{
				return null;
			}
					
		}
		catch (IndexMissingException e)
		{
			// the job does not exist
			String msg = "Missing Index no job with id " + jobId;
			s_Logger.error(msg);
		}
		
		return null;
	}


	@Override
	public boolean updateJob(String jobId, Map<String, Object> updates)
	throws UnknownJobException 
	{
		if (jobExists(jobId))
		{
			int retryCount = 3;
			while (--retryCount >= 0)
			{
				try
				{
					m_Client.prepareUpdate(jobId, JobDetails.TYPE, jobId)
										.setDoc(updates)
										.get();
					
					break;
				}
				catch (VersionConflictEngineException e)
				{
					s_Logger.warn("Conflict updating job document");
				}
			}
			
			if (retryCount <= 0)
			{
				s_Logger.warn("Unable to update conflicted job document " + jobId +
						". Updates = " + updates);
				return false;
			}
			
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
	public Pagination<Bucket> buckets(String jobId,
			boolean expand, int skip, int take,
			double anomalyScoreThreshold, double normalizedProbabilityThreshold)
	throws UnknownJobException
	{
		FilterBuilder fb = null;
		
		if (anomalyScoreThreshold > 0.0)
		{
			RangeFilterBuilder scoreFilter = FilterBuilders.rangeFilter(Bucket.ANOMALY_SCORE);
			scoreFilter.gte(anomalyScoreThreshold);
			fb = scoreFilter;
		}
		if (normalizedProbabilityThreshold > 0.0)
		{
			RangeFilterBuilder scoreFilter = FilterBuilders.rangeFilter(Bucket.MAX_NORMALIZED_PROBABILITY);
			scoreFilter.gte(normalizedProbabilityThreshold);
			
			if (fb == null)
			{
				fb = scoreFilter;
			}
			else 
			{
				fb = FilterBuilders.andFilter(scoreFilter, fb);
			}
			
		}
		
		if (fb == null)
		{
			fb = FilterBuilders.matchAllFilter();
		}
		
		return buckets(jobId, expand, skip, take, fb);
	}

	@Override
	public Pagination<Bucket> buckets(String jobId,
			boolean expand, int skip, int take, long startBucket, long endBucket,
			double anomalyScoreThreshold, double normalizedProbabilityThreshold)
	throws UnknownJobException
	{
		FilterBuilder fb = null;
		
		if (startBucket > 0 || endBucket > 0)
		{
			RangeFilterBuilder timeRange = FilterBuilders.rangeFilter(Bucket.TIMESTAMP);

			if (startBucket > 0)
			{
				timeRange.gte(startBucket);
			}
			if (endBucket > 0)
			{
				timeRange.lt(endBucket);
			}
			
			fb = timeRange;
		}
		
		
		if (anomalyScoreThreshold > 0.0)
		{
			RangeFilterBuilder scoreFilter = FilterBuilders.rangeFilter(Bucket.ANOMALY_SCORE);
			scoreFilter.gte(anomalyScoreThreshold);
			
			if (fb == null)
			{
				fb = scoreFilter;
			}
			else 
			{
				fb = FilterBuilders.andFilter(scoreFilter, fb);
			}
		}
		
		if (normalizedProbabilityThreshold > 0.0)
		{
			RangeFilterBuilder scoreFilter = FilterBuilders.rangeFilter(Bucket.MAX_NORMALIZED_PROBABILITY);
			scoreFilter.gte(normalizedProbabilityThreshold);
			
			if (fb == null)
			{
				fb = scoreFilter;
			}
			else 
			{
				fb = FilterBuilders.andFilter(scoreFilter, fb);
			}
		}

		return buckets(jobId, expand, skip, take, fb);
	}
	
	private Pagination<Bucket> buckets(String jobId, 
			boolean expand, int skip, int take,
			FilterBuilder fb) 
	throws UnknownJobException
	{	
		SortBuilder sb = new FieldSortBuilder(Bucket.ID)
					.ignoreUnmapped(true)
					.order(SortOrder.ASC);

		SearchResponse searchResponse;
		try
		{
			searchResponse = m_Client.prepareSearch(jobId)
										.setTypes(Bucket.TYPE)		
										.addSort(sb)
										.setPostFilter(fb)
										.setFrom(skip).setSize(take)
										.get();
		}
		catch (IndexMissingException e)
		{
			throw new UnknownJobException(jobId);
		}

		List<Bucket> results = new ArrayList<>();
		
		
		for (SearchHit hit : searchResponse.getHits().getHits())
		{
			// Remove the Kibana/Logstash '@timestamp' entry as stored in Elasticsearch, 
			// and replace using the API 'timestamp' key.
			Object timestamp = hit.getSource().remove(ElasticsearchMappings.ES_TIMESTAMP);
			hit.getSource().put(Bucket.TIMESTAMP, timestamp);

			Bucket bucket = m_ObjectMapper.convertValue(hit.getSource(), Bucket.class);

			if (expand)
			{
				int rskip = 0;
				int rtake = 500;
				Pagination<AnomalyRecord> page = this.bucketRecords(
						jobId, hit.getId(), rskip, rtake, 
						AnomalyRecord.PROBABILITY, false);
				
				if (page.getHitCount() > 0)
				{
					bucket.setRecords(page.getDocuments());
				}
				
				while (page.getHitCount() > rskip + rtake)
				{
					rskip += rtake;
					page = this.bucketRecords(
							jobId, hit.getId(), rskip, rtake, 
							AnomalyRecord.PROBABILITY, false);
					bucket.getRecords().addAll(page.getDocuments());
				}
			}

			results.add(bucket);
		}
		

		Pagination<Bucket> page = new Pagination<>();
		page.setDocuments(results);
		page.setHitCount(searchResponse.getHits().getTotalHits());
		page.setSkip(skip);
		page.setTake(take);

		return page;
	}
	
	@Override
	public SingleDocument<Bucket> bucket(String jobId, 
			String bucketId, boolean expand)
	throws UnknownJobException
	{
		GetResponse response;
		try
		{
			response = m_Client.prepareGet(jobId, Bucket.TYPE, bucketId).get();
		}
		catch (IndexMissingException e)
		{
			throw new UnknownJobException(jobId);
		}
		
		SingleDocument<Bucket> doc = new SingleDocument<>();
		doc.setType(Bucket.TYPE);
		doc.setDocumentId(bucketId);
		if (response.isExists())
		{
			// Remove the Kibana/Logstash '@timestamp' entry as stored in Elasticsearch, 
			// and replace using the API 'timestamp' key.
			Object timestamp = response.getSource().remove(ElasticsearchMappings.ES_TIMESTAMP);
			response.getSource().put(Bucket.TIMESTAMP, timestamp);

			Bucket bucket = m_ObjectMapper.convertValue(response.getSource(), Bucket.class);
			
			if (expand)
			{
				int rskip = 0;
				int rtake = 500;
				Pagination<AnomalyRecord> page = this.bucketRecords(
						jobId, bucketId, rskip, rtake, 
						AnomalyRecord.PROBABILITY, false);
				bucket.setRecords(page.getDocuments());
				
				while (page.getHitCount() > rskip + rtake)
				{
					rskip += rtake;
					page = this.bucketRecords(
							jobId, bucketId, rskip, rtake, 
							AnomalyRecord.PROBABILITY, false);
					bucket.getRecords().addAll(page.getDocuments());
				}
			}
			
			doc.setDocument(bucket);
		}
		
		return doc;
	}
	

	@Override
	public Pagination<AnomalyRecord> bucketRecords(String jobId,
			String bucketId, int skip, int take, String sortField, boolean descending)
	throws UnknownJobException
	{
		 FilterBuilder bucketFilter = FilterBuilders.hasParentFilter(Bucket.TYPE, 
								FilterBuilders.termFilter(Bucket.ID, bucketId));
		 
		 SortBuilder sb = null;
		 if (sortField != null)
		 {
			 sb = new FieldSortBuilder(sortField)
						 .ignoreUnmapped(true)
						 .missing("_last")
						 .order(descending ? SortOrder.DESC : SortOrder.ASC);		
		 }
		 
		 List<String> secondarySort = Arrays.asList(new String[] {
			 AnomalyRecord.ANOMALY_SCORE,
			 AnomalyRecord.OVER_FIELD_VALUE,
			 AnomalyRecord.PARTITION_FIELD_VALUE,
			 AnomalyRecord.BY_FIELD_VALUE,
			 AnomalyRecord.FIELD_NAME,
			 AnomalyRecord.FUNCTION}
		 );
		
		return records(jobId, skip, take, bucketFilter, sb, secondarySort,
				descending);
	}
	
	
	@Override
	public Pagination<AnomalyRecord> records(String jobId,
			int skip, int take,	long startBucket, long endBucket, 
			String sortField, boolean descending, 
			double anomalyScoreThreshold, double normalizedProbabilityThreshold)
	throws UnknownJobException
	{
		FilterBuilder fb = null;
		
		if (startBucket > 0 || endBucket > 0)
		{
			RangeFilterBuilder rangeFilter = FilterBuilders.rangeFilter(Bucket.TIMESTAMP);

			if (startBucket > 0)
			{
				rangeFilter.gte(startBucket);
			}
			if (endBucket > 0)
			{
				rangeFilter.lt(endBucket);
			}
			
			fb = FilterBuilders.hasParentFilter(Bucket.TYPE, rangeFilter);
		}
		
		if (anomalyScoreThreshold > 0.0)
		{
			RangeFilterBuilder scoreFilter = FilterBuilders.rangeFilter(AnomalyRecord.ANOMALY_SCORE);
			scoreFilter.gte(anomalyScoreThreshold);
			
			if (fb == null)
			{
				fb = scoreFilter;
			}
			else 
			{
				fb = FilterBuilders.andFilter(scoreFilter, fb);
			}
		}
		
		if (normalizedProbabilityThreshold > 0.0)
		{
			RangeFilterBuilder scoreFilter = FilterBuilders.rangeFilter(AnomalyRecord.NORMALIZED_PROBABILITY);
			scoreFilter.gte(normalizedProbabilityThreshold);
			
			if (fb == null)
			{
				fb = scoreFilter;
			}
			else 
			{
				fb = FilterBuilders.andFilter(scoreFilter, fb);
			}
		}
		

		return records(jobId, skip, take, fb, sortField, descending);
	}
	
	/**
	 * Light testing suggests that this method is actually 
	 * slower than querying each bucket individually.
	 * Best to query records by bucket id in a loop
	 */
	@Override
	public Pagination<AnomalyRecord> records(String jobId,
			List<String> bucketIds,  int skip, 
			int take, String sortField, boolean descending)
	throws UnknownJobException
	{
		IdsFilterBuilder idFilter = FilterBuilders.idsFilter(Bucket.TYPE);
		for (String id : bucketIds)
		{
			idFilter.addIds(id);
		}

		FilterBuilder bucketFilter = FilterBuilders.hasParentFilter(
				Bucket.TYPE, idFilter);


		return records(jobId, skip, take, bucketFilter, sortField, descending);
	}
	
	@Override
	public Pagination<AnomalyRecord> records(String jobId,
			int skip, int take, String sortField, boolean descending,
			double anomalyScoreThreshold, double normalizedProbabilityThreshold)
	throws UnknownJobException
	{
		 FilterBuilder fb = null;
		 
		 if (anomalyScoreThreshold > 0.0)
		 {
			 RangeFilterBuilder scoreFilter = FilterBuilders.rangeFilter(AnomalyRecord.ANOMALY_SCORE);
			 scoreFilter.gte(anomalyScoreThreshold);
			 fb = scoreFilter;
		 }
		 if (normalizedProbabilityThreshold > 0.0)
		 {
			 RangeFilterBuilder scoreFilter = FilterBuilders.rangeFilter(AnomalyRecord.NORMALIZED_PROBABILITY);
			 scoreFilter.gte(normalizedProbabilityThreshold);

			 if (fb == null)
			 {
				 fb = scoreFilter;
			 }
			 else 
			 {
				 fb = FilterBuilders.andFilter(scoreFilter, fb);
			 }
		 }

		 if (fb == null)
		 {
			 fb = FilterBuilders.matchAllFilter();
		 }
		 
		return records(jobId, skip, take, fb, sortField, descending);
	}
	
	
	private Pagination<AnomalyRecord> records(String jobId,
			int skip, int take, FilterBuilder recordFilter,
			String sortField, boolean descending) 
    throws UnknownJobException
	{
		 SortBuilder sb = null;
		 if (sortField != null)
		 {
			 sb = new FieldSortBuilder(sortField)
						 .ignoreUnmapped(true)
						 .missing("_last")
						 .order(descending ? SortOrder.DESC : SortOrder.ASC);		
		 }
		 
		return records(jobId, skip, take, recordFilter, sb, SECONDARY_SORT, descending);
	}
	
	
	/**
	 * The returned records have the parent bucket id set.
	 */
	private Pagination<AnomalyRecord> records(String jobId, int skip, int take,
			FilterBuilder recordFilter, SortBuilder sb, List<String> secondarySort,
			boolean descending) 
	throws UnknownJobException
	{
		SearchRequestBuilder searchBuilder = m_Client.prepareSearch(jobId)
				.setTypes(AnomalyRecord.TYPE)
				.setPostFilter(recordFilter)
				.setFrom(skip).setSize(take)
				.addField(_PARENT)   // include the parent id
				.setFetchSource(true);  // the field option turns off source so request it explicitly
		
		
		if (sb != null)
		{
			searchBuilder.addSort(sb);
		}
		
		for (String sortField : secondarySort)
		{
			searchBuilder.addSort(sortField, descending ? SortOrder.DESC : SortOrder.ASC);
		}

		
		SearchResponse searchResponse;
		try
		{
			searchResponse = searchBuilder.get();
		}
		catch (IndexMissingException e)
		{
			throw new UnknownJobException(jobId);
		}

		List<AnomalyRecord> results = new ArrayList<>();
		for (SearchHit hit : searchResponse.getHits().getHits())
		{
			Map<String, Object> m  = hit.getSource();

			// replace logstash timestamp name with timestamp
			m.put(Bucket.TIMESTAMP, m.remove(ElasticsearchMappings.ES_TIMESTAMP));
			
			AnomalyRecord record = m_ObjectMapper.convertValue(
					m, AnomalyRecord.class);

			// set the ID and parent ID
			record.setId(hit.getId());
			record.setParent(hit.field(_PARENT).getValue().toString());
			
			results.add(record);
		}
		
		Pagination<AnomalyRecord> page = new Pagination<>();
		page.setDocuments(results);
		page.setHitCount(searchResponse.getHits().getTotalHits());
		page.setSkip(skip);
		page.setTake(take);
		
		return page;	
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


	@Override
	public QuantilesState getQuantilesState(String jobId)
	throws UnknownJobException
	{
		QuantilesState quantilesState = new QuantilesState();

		MultiGetRequestBuilder multiGetRequestBuilder = m_Client.prepareMultiGet()
				.add(jobId, Quantiles.TYPE, QuantilesState.SYS_CHANGE_QUANTILES_KIND)
				.add(jobId, Quantiles.TYPE, QuantilesState.UNUSUAL_QUANTILES_KIND);

		try
		{
			MultiGetResponse multiGetResponse = multiGetRequestBuilder.get();

			for (MultiGetItemResponse response : multiGetResponse.getResponses())
			{
				String kind = response.getId();
				if (response.isFailed() || !response.getResponse().isExists())
				{
					s_Logger.info("There are currently no " + kind +
									" quantiles for job " + jobId);
				}
				else
				{
					Object state = response.getResponse().getSource().get(Quantiles.QUANTILE_STATE);
					if (state == null)
					{
						s_Logger.error("Inconsistency - no " + Quantiles.QUANTILE_STATE +
										" field in " + kind +
										" quantiles for job " + jobId);
					}
					else
					{
						quantilesState.setQuantilesState(kind, state.toString());
					}
				}
			}
		}
		catch (IndexMissingException e)
		{
			s_Logger.error("Unknown job '" + jobId + "'. Cannot read persisted state");
			throw new UnknownJobException(jobId,
					"Cannot read persisted state", ErrorCode.MISSING_DETECTOR_STATE);
		}
		return quantilesState;
	}
}

