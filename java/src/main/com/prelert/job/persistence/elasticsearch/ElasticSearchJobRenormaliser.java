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

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Client;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.JobDetails;
import com.prelert.job.UnknownJobException;
import com.prelert.job.normalisation.Normaliser;
import com.prelert.job.persistence.JobRenormaliser;
import com.prelert.job.process.NativeProcessRunException;
import com.prelert.rs.data.AnomalyRecord;
import com.prelert.rs.data.Bucket;


/**
 * Updates {@linkplain Bucket Buckets} and their contained
 * {@linkplain AnomalyRecord AnomalyRecords} with new normalised
 * anomaly scores and unusual scores
 */
public class ElasticSearchJobRenormaliser implements JobRenormaliser
{
	private String m_JobId;
	private ElasticSearchJobProvider m_JobProvider;

	/**
	 * This read from the data store on first access
	 */
	private int m_BucketSpan;


	/**
	 * Create with the ElasticSearch client. Data will be written to
	 * the index <code>jobId</code>
	 *
	 * @param jobId The job Id/ElasticSearch index
	 * @param jobProvider The ElasticSearch job provider
	 */
	public ElasticSearchJobRenormaliser(String jobId, ElasticSearchJobProvider jobProvider)
	{
		m_JobId = jobId;
		m_JobProvider = jobProvider;
		m_BucketSpan = 0;
	}


	/**
	 * Update the anomaly score field on all previously persisted buckets
	 * and all contained records
	 * @param sysChangeState
	 * @param logger
	 */
	@Override
	public void updateBucketSysChange(String sysChangeState,
										Logger logger)
	{
		try
		{
			List<Bucket> buckets = m_JobProvider.buckets(m_JobId,
						true, 0, Integer.MAX_VALUE).getDocuments();
			if (buckets == null)
			{
				return;
			}

			Normaliser normaliser = new Normaliser(m_JobId, m_JobProvider, logger);
			List<Bucket> normalisedBuckets =
					normaliser.normaliseForSystemChange(getJobBucketSpan(),
													buckets, sysChangeState);

			for (Bucket bucket : normalisedBuckets)
			{
				updateSingleBucket(bucket, true, false, logger);
			}
		}
		catch (UnknownJobException uje)
		{
			logger.error("Inconsistency - job " + m_JobId +
							" unknown during renormalisation", uje);
		}
		catch (NativeProcessRunException npe)
		{
			logger.error("Failed to renormalise for system changes", npe);
		}
	}


	/**
	 * Update the unsual score field on all previously persisted buckets
	 * and all contained records
	 * @param unusualBehaviourState
	 * @param logger
	 */
	@Override
	public void updateBucketUnusualBehaviour(String unusualBehaviourState,
											Logger logger)
	{
		try
		{
			List<Bucket> buckets = m_JobProvider.buckets(m_JobId,
						true, 0, Integer.MAX_VALUE).getDocuments();
			if (buckets == null)
			{
				return;
			}

			Normaliser normaliser = new Normaliser(m_JobId, m_JobProvider, logger);
			List<Bucket> normalisedBuckets =
					normaliser.normaliseForUnusualBehaviour(getJobBucketSpan(),
													buckets, unusualBehaviourState);

			for (Bucket bucket : normalisedBuckets)
			{
				updateSingleBucket(bucket, false, true, logger);
			}
		}
		catch (UnknownJobException uje)
		{
			logger.error("Inconsistency - job " + m_JobId +
							" unknown during renormalisation", uje);
		}
		catch (NativeProcessRunException npe)
		{
			logger.error("Failed to renormalise for unusual behaviour", npe);
		}
	}


	/**
	 * Update the anomaly score and unsual score fields on the bucket provided
	 * and all contained records
	 * @param bucket
	 * @param updateSysChange
	 * @param updateUnusual
	 */
	private void updateSingleBucket(Bucket bucket,
			boolean updateSysChange, boolean updateUnusual,
			Logger logger)
	{
		Map<String, Object> map = new TreeMap<>();
		if (updateSysChange)
		{
			map.put(Bucket.ANOMALY_SCORE, bucket.getAnomalyScore());
		}
		if (updateUnusual)
		{
			map.put(Bucket.UNUSUAL_SCORE, bucket.getUnusualScore());
		}

		try
		{
			// First update the bucket
			String id = bucket.getId();
			if (id != null)
			{
				m_JobProvider.getClient().prepareUpdate(m_JobId, Bucket.TYPE, id)
						// TODO add when we upgrade to ES 1.3
						//.setDetectNoop(true)
						.setDoc(map)
						.execute().actionGet();
			}
			else
			{
				logger.warn("Failed to renormalise bucket - no ID");
			}

			// Now update the records within the bucket
			map.clear();
			for (AnomalyRecord record : bucket.getRecords())
			{
				if (updateSysChange)
				{
					map.put(AnomalyRecord.ANOMALY_SCORE, record.getAnomalyScore());
				}
				if (updateUnusual)
				{
					map.put(AnomalyRecord.UNUSUAL_SCORE, record.getUnusualScore());
				}

				id = record.getId();
				if (id != null)
				{
					m_JobProvider.getClient().prepareUpdate(m_JobId, AnomalyRecord.TYPE, id)
							// TODO add when we upgrade to ES 1.3
							//.setDetectNoop(true)
							.setDoc(map)
							.execute().actionGet();
				}
				else
				{
					logger.warn("Failed to renormalise record - no ID");
				}
			}
		}
		catch (ElasticsearchException e)
		{
			logger.error("Error updating bucket state", e);
		}
	}


	synchronized private int getJobBucketSpan()
	{
		if (m_BucketSpan == 0)
		{
			// use dot notation to get fields from nested docs.
			Number num = m_JobProvider.<Number>getField(m_JobId,
					JobDetails.ANALYSIS_CONFIG + "." + AnalysisConfig.BUCKET_SPAN);
			if (num != null)
			{
				m_BucketSpan = num.intValue();
			}
		}

		return m_BucketSpan;
	}
};

