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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;

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
 * anomaly scores and unusual scores.
 *
 * This is done in a separate thread to avoid blocking the main
 * data processing during renormalisations.
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
	 * Queue of updated quantiles to be used for renormalisation
	 */
	private BlockingQueue<QuantileInfo> m_UpdatedQuantileQueue;

	/**
	 * Thread to use so that quantile updates can run in parallel to the main
	 * data processing of the job
	 */
	private Thread m_QuantileUpdateThread;

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
		// Queue limit of 50 means that renormalisation will eventually block
		// the main data processing of the job if it gets too far ahead
		m_UpdatedQuantileQueue = new ArrayBlockingQueue<>(50);
		m_QuantileUpdateThread = new Thread(this.new QueueDrainer(),
			m_JobId + "-Renormalizer");
		m_QuantileUpdateThread.start();
	}


	/**
	 * Shut down the worker thread
	 */
	synchronized public boolean shutdown(Logger logger)
	{
		try
		{
			m_UpdatedQuantileQueue.add(new QuantileInfo(QuantileInfo.InfoType.END,
					"", logger));
			m_QuantileUpdateThread.join();
			m_UpdatedQuantileQueue.clear();
		}
		catch (InterruptedException e)
		{
			return false;
		}

		return true;
	}


	/**
	 * Update the anomaly score field on all previously persisted buckets
	 * and all contained records
	 * @param sysChangeState
	 * @param logger
	 */
	@Override
	synchronized public void updateBucketSysChange(String sysChangeState,
										Logger logger)
	{
		if (m_QuantileUpdateThread.isAlive())
		{
			m_UpdatedQuantileQueue.add(new QuantileInfo(QuantileInfo.InfoType.SYS_CHANGE,
					sysChangeState, logger));
		}
		else
		{
			logger.error("Cannot renormalise for system changes " +
						"- update thread no longer running");
		}
	}


	/**
	 * Update the anomaly score field on all previously persisted buckets
	 * and all contained records
	 * @param sysChangeState
	 * @param logger
	 */
	public void doSysChangeUpdate(String sysChangeState,
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

			int[] counts = { 0, 0 };
			for (Bucket bucket : normalisedBuckets)
			{
				updateSingleBucket(bucket, true, false, logger, counts);
			}
			logger.info("System changes normalisation resulted in: " +
						counts[0] + " updates, " +
						counts[1] + " no-ops");
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
	synchronized public void updateBucketUnusualBehaviour(String unusualBehaviourState,
											Logger logger)
	{
		if (m_QuantileUpdateThread.isAlive())
		{
			m_UpdatedQuantileQueue.add(new QuantileInfo(QuantileInfo.InfoType.UNUSUAL,
					unusualBehaviourState, logger));
		}
		else
		{
			logger.error("Cannot renormalise for unusual behaviour " +
						"- update thread no longer running");
		}
	}


	/**
	 * Update the unsual score field on all previously persisted buckets
	 * and all contained records
	 * @param unusualBehaviourState
	 * @param logger
	 */
	private void doUnusualBehaviourUpdate(String unusualBehaviourState,
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

			int[] counts = { 0, 0 };
			for (Bucket bucket : normalisedBuckets)
			{
				updateSingleBucket(bucket, false, true, logger, counts);
			}
			logger.info("Unusual behaviour normalisation resulted in: " +
						counts[0] + " updates, " +
						counts[1] + " no-ops");
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
	 * @param logger
	 * @param counts Element 0 will be incremented if we update a document and
	 * element 1 if we don't
	 */
	private void updateSingleBucket(Bucket bucket,
			boolean updateSysChange, boolean updateUnusual,
			Logger logger, int[] counts)
	{
		try
		{
			// First update the bucket if worthwhile
			String bucketId = bucket.getId();
			if (bucketId != null)
			{
				if (bucket.hadBigNormalisedUpdate())
				{
					Map<String, Object> map = new TreeMap<>();
					if (updateSysChange)
					{
						map.put(Bucket.ANOMALY_SCORE, bucket.getAnomalyScore());
					}
					if (updateUnusual)
					{
						map.put(Bucket.MAX_RECORD_UNUSUALNESS, bucket.getMaxRecordUnusualness());
					}

					m_JobProvider.getClient().prepareUpdate(m_JobId, Bucket.TYPE, bucketId)
							.setDoc(map)
							.execute().actionGet();

					++counts[0];
				}
				else
				{
					++counts[1];
				}
			}
			else
			{
				logger.warn("Failed to renormalise bucket - no ID");
				++counts[1];
			}

			// Now bulk update the records within the bucket
			BulkRequestBuilder bulkRequest = m_JobProvider.getClient().prepareBulk();
			boolean addedAny = false;
			for (AnomalyRecord record : bucket.getRecords())
			{
				String recordId = record.getId();
				if (recordId != null)
				{
					if (record.hadBigNormalisedUpdate())
					{
						Map<String, Object> map = new TreeMap<>();
						if (updateSysChange)
						{
							map.put(AnomalyRecord.ANOMALY_SCORE, record.getAnomalyScore());
						}
						if (updateUnusual)
						{
							map.put(AnomalyRecord.RECORD_UNUSUALNESS, record.getRecordUnusualness());
						}

						bulkRequest.add(m_JobProvider.getClient()
								.prepareUpdate(m_JobId, AnomalyRecord.TYPE, recordId)
								.setDoc(map)
								// Need to specify the parent ID when updating a child
								.setParent(bucketId));

						addedAny = true;
						++counts[0];
					}
					else
					{
						++counts[1];
					}
				}
				else
				{
					logger.warn("Failed to renormalise record - no ID");
					++counts[1];
				}
			}

			if (addedAny)
			{
				BulkResponse bulkResponse = bulkRequest.execute().actionGet();
				if (bulkResponse.hasFailures())
				{
					logger.error("BulkResponse has errors");
					for (BulkItemResponse item : bulkResponse.getItems())
					{
						logger.error(item.getFailureMessage());
					}
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


	/**
	 * Simple class to group information in the blocking queue
	 */
	static private class QuantileInfo
	{
		public enum InfoType { END, SYS_CHANGE, UNUSUAL };

		public InfoType m_Type;
		public String m_State;
		public Logger m_Logger;

		public QuantileInfo(InfoType type,
				String state, Logger logger)
		{
			m_Type = type;
			m_State = state;
			m_Logger = logger;
		}
	};


	/**
	 * Thread handler to drain updated quantiles from the blocking queue and
	 * trigger renormalisations in response
	 *
	 * The logic is to drain as many info objects from the queue as possible
	 * until an end marker is reached.  Any info objects after the first end
	 * marker are discarded.  The benefit in draining as many objects as
	 * possible each time the loop runs is that if normalisation is taking
	 * much longer than the main processing and multiple sets of quantiles
	 * of the same type are in the queue we can ignore all but the most
	 * recent (i.e. those nearest the back of the queue).
	 */
	private class QueueDrainer implements Runnable
	{
		public void run()
		{
			try
			{
				boolean keepGoing = true;
				while (keepGoing)
				{
					QuantileInfo latestSysChangeInfo = null;
					QuantileInfo latestUnusualInfo = null;

					// take() will block if the queue is empty
					QuantileInfo info = ElasticsearchJobRenormaliser.this.m_UpdatedQuantileQueue.take();
					while (keepGoing && info != null)
					{
						switch (info.m_Type)
						{
							case END:
								keepGoing = false;
								info.m_Logger.info("Normaliser thread received end instruction");
								break;
							case SYS_CHANGE:
								if (latestSysChangeInfo != null)
								{
									latestSysChangeInfo.m_Logger.info("System change quantiles superseded before processing");
								}
								latestSysChangeInfo = info;
								break;
							case UNUSUAL:
								if (latestUnusualInfo != null)
								{
									latestUnusualInfo.m_Logger.info("Unusual behaviour quantiles superseded before processing");
								}
								latestUnusualInfo = info;
								break;
						}
						// poll() will return null if the queue is empty
						info = ElasticsearchJobRenormaliser.this.m_UpdatedQuantileQueue.poll();
					}

					if (latestSysChangeInfo != null)
					{
						ElasticsearchJobRenormaliser.this.doSysChangeUpdate(latestSysChangeInfo.m_State,
								latestSysChangeInfo.m_Logger);
					}
					if (latestUnusualInfo != null)
					{
						ElasticsearchJobRenormaliser.this.doUnusualBehaviourUpdate(latestUnusualInfo.m_State,
								latestUnusualInfo.m_Logger);
					}
				}
			}
			catch (InterruptedException e)
			{
				// Thread will exit now
			}
		}
	};
};

