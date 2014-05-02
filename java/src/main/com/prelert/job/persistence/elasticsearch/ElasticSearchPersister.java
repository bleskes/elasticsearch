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

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;

import com.prelert.job.DetectorState;
import com.prelert.job.UnknownJobException;
import com.prelert.job.persistence.JobDataPersister;
import com.prelert.rs.data.AnomalyRecord;
import com.prelert.rs.data.Bucket;
import com.prelert.rs.data.Detector;
import com.prelert.rs.data.ErrorCode;

/**
 * Saves result Buckets and DetectorState to ElasticSearch<br/>
 * 
 * <b>Buckets</b> are written with the following structure:
 * <h2>Bucket</h2>
 * The results of each job are stored in buckets, this is the top level 
 * structure for the results. A bucket contains multiple anomaly records.
 * The anomaly score of the bucket may not match the summed score of all
 * the records as all the records may not have been outputted for the bucket.
 * <h2>Anomaly Record</h2>
 * In ElasticSearch records have a parent <-> child relationship with
 * buckets and should only exist is relation to a parent bucket. Each record
 * was generated by a detector which comes from its detector name field.
 * <h2>Detector</h2>
 * The Job has a fixed number of detectors but there may not be output 
 * for every detector in each bucket. The detector has a name/key every
 * record has a detector name field so you can search for records by
 * detector<br/>
 * <br/>
 * <b>DetectorState</b> may contain model state for multiple detectors each of 
 * which is stored in a separate document of type {@link DetectorState.TYPE} 
 * <br/>
 * @see com.prelert.job.persistence.elasticsearch.ElasticSearchMappings
 */
public class ElasticSearchPersister implements JobDataPersister
{
	static public final Logger s_Logger = Logger.getLogger(ElasticSearchPersister.class);
	
	private Client m_Client;
	private String m_JobId;
	
	private Set<String> m_DetectorNames;
		
	/**
	 * Create the with the ElasticSearch client. Data will be written
	 * the index <code>jobId</code>
	 * 
	 * @param jobId The job Id/ElasticSearch index
	 * @param client The ElasticSearch client
	 */
	public ElasticSearchPersister(String jobId, Client client)
	{
		m_JobId = jobId;
		m_Client = client;
		m_DetectorNames = new HashSet<String>();
	}
	
	/**
	 * This implementation tracks detector names and only writes detectors
	 * to the database when it sees one it hasn't seen before. If the 
	 * same instance is used to write all buckets in the job then the 
	 * detectors will only be written once else they will be written multiple
	 * times and ElasticSearch will overwrite them creating new versions of
	 * the document but the end result is the same. 
	 */
	@Override
	public void persistBucket(Bucket bucket) 
	{
		try 
		{
			XContentBuilder content = serialiseBucket(bucket);
			
			m_Client.prepareIndex(m_JobId, Bucket.TYPE, bucket.getEpochString())
					.setSource(content)
					.execute().actionGet();
			
			/* TODO this method is only in version ElasticSearch 1.0
			if (response.isCreated() == false)
			{
				s_Logger.error(String.format("Bucket %s document not created",
						bucket.getId()));
			}
			*/
			
			for (Detector detector : bucket.getDetectors())
			{								
				if (m_DetectorNames.contains(detector.getName()) == false)
				{
					m_DetectorNames.add(detector.getName());
					// Write the detector
					content = serialiseDetector(detector);
					m_Client.prepareIndex(m_JobId, Detector.TYPE, detector.getName())
						.setSource(content)
						.get();				
				}
				
				/* TODO this method is only in version ElasticSearch 1.0
				if (response.isCreated() == false)
				{
					s_Logger.error(String.format("Detector %s document not created",
							detectorId));
				}
				*/
				
				BulkRequestBuilder bulkRequest = m_Client.prepareBulk();
				int count = 1;
				for (AnomalyRecord record : detector.getRecords())
				{
					content = serialiseRecord(record, detector.getName(), bucket.getTimestamp());
					
					String recordId = bucket.getEpoch() + detector.getName() + count;					
					bulkRequest.add(m_Client.prepareIndex(m_JobId, AnomalyRecord.TYPE, recordId)
							.setSource(content)
							.setParent(bucket.getEpochString()));					
					++count;
				}
				
				BulkResponse bulkResponse = bulkRequest.execute().actionGet();
				if (bulkResponse.hasFailures())
				{
					s_Logger.error("BulkResponse has errors");
					for (BulkItemResponse item : bulkResponse.getItems())
					{
						s_Logger.error(item.getFailureMessage());
					}
				}
			}	
			
		}
		catch (IOException e) 
		{
			s_Logger.error("Error writing bucket state", e);
		}
	}
	
	/**
	 * The state object can contain multiple detector states each of which 
	 * will be written to an separate document. For each ES index, 
	 * which corresponds to a job, there can  only be 1 serialised state for 
	 * each detector. If the detectors state is updated the last state is 
	 * simply overwritten.
	 * @param state If <code>null</code> then returns straight away.
	 * @throws IOException 
	 */
	@Override
	public void persistDetectorState(DetectorState state) 
	{
		if (state == null)
		{
			s_Logger.warn("No detector state to persist for job " + m_JobId);
			return;
		}
		
		try
		{
			for (Map.Entry<String, String> entry : state.getMap().entrySet())
			{
				XContentBuilder content = serialiseDetectorState(entry.getKey(),
						entry.getValue());

				m_Client.prepareIndex(m_JobId, DetectorState.TYPE, entry.getKey())
					.setSource(content)
					.execute().actionGet();

				/* TODO this method is only in version ElasticSearch 1.0
				if (response.isCreated() == false)
				{
					s_Logger.error(String.format("Bucket %s document not created",
							bucket.getId()));
				}
				*/
			}
		}
		catch (IOException e) 
		{
			s_Logger.error("Error writing detector state", e);
		}
	}

	@Override
	public boolean isDetectorStatePersisted() 
	{
		FilterBuilder fb = FilterBuilders.matchAllFilter();
		// don't return for any documents (size = 0), just get hit count
		SearchRequestBuilder searchBuilder = m_Client.prepareSearch(m_JobId)
				.setTypes(DetectorState.TYPE)		
				.setPostFilter(fb)
				.setFrom(0).setSize(0);
		
		SearchResponse searchResponse = searchBuilder.get();		
		return searchResponse.getHits().totalHits() > 0;
	}
	
	/**
	 * Refreshes the elastic search index
	 * @return
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	@Override 
	public boolean commitWrites() 
	{
		// refresh the index so the buckets are immediately searchable
		m_Client.admin().indices().refresh(new RefreshRequest(m_JobId)).actionGet();	
		return true;		
	}
	
	/**
	 * Get the names of the detectors that have had their state 
	 * persisted to the database.
	 * 
	 * @return List of names or empty 
	 */
	public List<String> persistedDetectorNames() 
	{
		FilterBuilder fb = FilterBuilders.matchAllFilter();
		
		List<String> names = new ArrayList<>();
		
		SearchRequestBuilder searchBuilder = m_Client.prepareSearch(m_JobId)
				.setTypes(DetectorState.TYPE)		
				.setPostFilter(fb)
				.addField(DetectorState.DETECTOR_NAME);
		
		
		final int PAGE_SIZE = 50;		
		int from=0, size=PAGE_SIZE;
		boolean getNextPage = true;
		while (getNextPage)
		{
			searchBuilder.setFrom(from).setSize(size); 
			SearchResponse searchResponse = searchBuilder.get();
						
			for (SearchHit hit : searchResponse.getHits().getHits())
			{
				names.add(hit.field(DetectorState.DETECTOR_NAME).getValue().toString());
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
		
		return names;
	}
	
	/**
	 * Reads all the detector state documents from 
	 * the database and returns a {@linkplain DetectorState} object.
	 * 
	 * @return
	 * @throws UnknownJobException if the job id is not recognised
	 */
	public DetectorState retrieveDetectorState()
	throws UnknownJobException
	{
		DetectorState detectorState = new DetectorState();
		
		FilterBuilder fb = FilterBuilders.matchAllFilter();
		
		SearchRequestBuilder searchBuilder = m_Client.prepareSearch(m_JobId)
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
			s_Logger.error("Unknown job '" + m_JobId + "'. Cannot read persisted state");
			throw new UnknownJobException(m_JobId, 
					"Cannot read persisted state", ErrorCode.MISSING_DETECTOR_STATE);
		}
		return detectorState;
	}

	
	/**
	 * Return the bucket as serialisable content
	 * @param bucket
	 * @return
	 * @throws IOException
	 */
	private XContentBuilder serialiseBucket(Bucket bucket) 
	throws IOException
	{
		XContentBuilder builder = jsonBuilder().startObject()
				.field(Bucket.ID, bucket.getId())
				.field(Bucket.TIMESTAMP, bucket.getTimestamp())
				.field(Bucket.ANOMALY_SCORE, bucket.getAnomalyScore())
				.field(Bucket.RECORD_COUNT, bucket.getRecordCount())
				.endObject();
		
		return builder;
	}
	
	/**
	 * Return the detector as serialisable content
	 * @param detector
	 * @return
	 * @throws IOException
	 */
	private XContentBuilder serialiseDetector(Detector detector) 
	throws IOException
	{
		XContentBuilder builder = jsonBuilder().startObject()
				.field(Detector.NAME, detector.getName())
				.endObject();
				
		return builder;
	}
	
	/**
	 * Return the anomaly record as serialisable content
	 * 
	 * @param record Record to serialise
	 * @param detectorKey The detector's name
	 * @param bucketTime The timestamp of the anomaly record parent bucket
	 * @return
	 * @throws IOException
	 */
	private XContentBuilder serialiseRecord(AnomalyRecord record, String detectorKey, Date bucketTime) 
	throws IOException
	{		
		XContentBuilder builder = jsonBuilder().startObject()
				.field(AnomalyRecord.ANOMALY_SCORE, record.getAnomalyScore())
				.field(AnomalyRecord.PROBABILITY, record.getProbability())
				.field(AnomalyRecord.DETECTOR_NAME, detectorKey)
				.field(Bucket.TIMESTAMP, bucketTime);

		if (record.getByFieldName() != null)
		{
			builder.field(AnomalyRecord.BY_FIELD_NAME, record.getByFieldName());
		}
		if (record.getByFieldValue() != null)
		{
			builder.field(AnomalyRecord.BY_FIELD_VALUE, record.getByFieldValue());
		}
		if (record.getTypical() != null)
		{
			builder.field(AnomalyRecord.TYPICAL, record.getTypical());
		}
		if (record.getActual() != null)
		{
			builder.field(AnomalyRecord.ACTUAL, record.getActual());
		}
		if (record.getFieldName() != null)
		{
			builder.field(AnomalyRecord.FIELD_NAME, record.getFieldName());
		}
		if (record.getFunction() != null)
		{
			builder.field(AnomalyRecord.FUNCTION, record.getFunction());
		}
		if (record.getPartitionFieldName() != null)
		{
			builder.field(AnomalyRecord.PARTITION_FIELD_NAME, record.getPartitionFieldName());
		}
		if (record.getPartitionFieldValue() != null)
		{
			builder.field(AnomalyRecord.PARTITION_FIELD_VALUE, record.getPartitionFieldValue());
		}			
		if (record.getOverFieldName() != null)
		{
			builder.field(AnomalyRecord.OVER_FIELD_NAME, record.getOverFieldName());
		}	
		if (record.getOverFieldValue() != null)
		{
			builder.field(AnomalyRecord.OVER_FIELD_VALUE, record.getOverFieldValue());
		}	
		if (record.isOverallResult() != null)
		{
			builder.field(AnomalyRecord.IS_OVERALL_RESULT, record.isOverallResult());
		}	
		if (record.isSimpleCount() != null)
		{
			builder.field(AnomalyRecord.IS_SIMPLE_COUNT, record.isSimpleCount());
		}


		builder.endObject();
		
		return builder;
	}
	
	/**
	 * Return the detector model state as content that can be written to
	 * ElasticSearch
	 * 
	 * @param detectorName 
	 * @param modelState The serialised model Xml
	 * @return
	 * @throws IOException
	 */
	private XContentBuilder serialiseDetectorState(String detectorName,
			String modelState) 
	throws IOException
	{
		XContentBuilder builder = jsonBuilder().startObject()
					.field(DetectorState.DETECTOR_NAME, detectorName)
					.field(DetectorState.SERIALISED_MODEL, modelState)
				.endObject();
				
		return builder;
	}
}
