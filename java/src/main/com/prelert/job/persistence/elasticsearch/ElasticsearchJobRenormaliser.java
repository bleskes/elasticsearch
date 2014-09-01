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


import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.elasticsearch.ElasticsearchException;

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
public class ElasticsearchJobRenormaliser implements JobRenormaliser
{
	private String m_JobId;
	private ElasticsearchJobProvider m_JobProvider;

	/**
	 * This read from the data store on first access
	 */
	private int m_BucketSpan;

	/**
	 * Maximum number of buckets to renormalise at a time
	 */
	private static final int MAX_BUCKETS = 100000;

	/**
	 * Create with the Elasticsearch client. Data will be written to
	 * the index <code>jobId</code>
	 *
	 * @param jobId The job Id/Elasticsearch index
	 * @param jobProvider The Elasticsearch job provider
	 */
	public ElasticsearchJobRenormaliser(String jobId, ElasticsearchJobProvider jobProvider)
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
						true, 0, MAX_BUCKETS, 0.0, 0.0).getDocuments();
			if (buckets == null)
			{
				logger.warn("No existing buckets to renormalise for job " +
							m_JobId);
				return;
			}

			Normaliser normaliser = new Normaliser(m_JobId, m_JobProvider, logger);
			List<Bucket> normalisedBuckets =
					normaliser.normaliseForSystemChange(getJobBucketSpan(logger),
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
						true, 0, MAX_BUCKETS, 0.0, 0.0).getDocuments();
			if (buckets == null)
			{
				logger.warn("No existing buckets to renormalise for job " +
							m_JobId);
				return;
			}

			Normaliser normaliser = new Normaliser(m_JobId, m_JobProvider, logger);
			List<Bucket> normalisedBuckets =
					normaliser.normaliseForUnusualBehaviour(getJobBucketSpan(logger),
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
			map.put(Bucket.MAX_RECORD_UNUSUALNESS, bucket.getUnusualScore());
		}

		try
		{
			// First update the bucket
			String bucketId = bucket.getId();
			if (bucketId != null)
			{
				m_JobProvider.getClient().prepareUpdate(m_JobId, Bucket.TYPE, bucketId)
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
					map.put(AnomalyRecord.RECORD_UNUSUALNESS, record.getUnusualScore());
				}

				String recordId = record.getId();
				if (recordId != null)
				{
					m_JobProvider.getClient().prepareUpdate(m_JobId, AnomalyRecord.TYPE, recordId)
							// TODO add when we upgrade to ES 1.3
							//.setDetectNoop(true)
							.setDoc(map)
							// Need to specify the parent ID when updating a child
							.setParent(bucketId)
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


	synchronized private int getJobBucketSpan(Logger logger)
	{
		if (m_BucketSpan == 0)
		{
			// use dot notation to get fields from nested docs.
			Number num = m_JobProvider.<Number>getField(m_JobId,
					JobDetails.ANALYSIS_CONFIG + "." + AnalysisConfig.BUCKET_SPAN);
			if (num != null)
			{
				m_BucketSpan = num.intValue();
				logger.info("Caching bucket span " + m_BucketSpan +
							" for job " + m_JobId);
			}
		}

		return m_BucketSpan;
	}
};

