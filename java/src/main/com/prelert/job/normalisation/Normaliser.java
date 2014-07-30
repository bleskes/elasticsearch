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

package com.prelert.job.normalisation;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.prelert.job.input.LengthEncodedWriter;
import com.prelert.job.persistence.JobResultsProvider;
import com.prelert.job.process.NativeProcessRunException;
import com.prelert.job.process.ProcessCtrl;
import com.prelert.job.process.output.NormalisedResultsParser;
import com.prelert.rs.data.AnomalyRecord;
import com.prelert.rs.data.Bucket;
import com.prelert.rs.data.ErrorCode;

/**
 * Normalises bucket scores and anomaly records for either 
 * System Change, Unusual behaviour or both.
 * <br/>
 * Creates and initialises the normaliser process, pipes the probabilities/
 * anomaly scores through them and adds the normalised values to 
 * the records/buckets.
 */
public class Normaliser 
{
	private JobResultsProvider m_JobDetailsProvider;
	
	private String m_JobId;

	private Logger m_Logger; 
	
	public Normaliser(String jobId, JobResultsProvider jobResultsProvider, 
			Logger logger)
	{
		m_JobDetailsProvider = jobResultsProvider;
		m_JobId = jobId;
		m_Logger = logger;
	}
			
	/**	 
	 * Normalise buckets anomaly score for system state change.
	 * 
	 * @param bucketSpan If <code>null</code> the default is used
	 * @param buckets Will be modified to have the normalised result
	 * @return
	 * @throws NativeProcessRunException
	 */
	public List<Bucket> normaliseForSystemChange(Integer bucketSpan, 
			List<Bucket> buckets) 
	throws NativeProcessRunException
	{
		InitialState state = m_JobDetailsProvider.getSystemChangeInitialiser(m_JobId);
		
		NormaliserProcess process = createNormaliserProcess(
				state, null, bucketSpan);
		
		NormalisedResultsParser resultsParser = new NormalisedResultsParser(
							process.getProcess().getInputStream(),
							m_Logger);
		
		Thread parserThread = new Thread(resultsParser, m_JobId + "-Normalizer-Parser");
		parserThread.start();
						
		LengthEncodedWriter writer = new LengthEncodedWriter(
				process.getProcess().getOutputStream());
		
		try 
		{
			writer.writeNumFields(2);
			writer.writeField("anomalyScore");
			writer.writeField("tag");
			
			for (Bucket bucket : buckets)
			{
				writer.writeNumFields(2);
				writer.writeField(Double.toString(bucket.getAnomalyScore()));
				writer.writeField(bucket.getId());
			}
		}
		catch (IOException e) 
		{
			m_Logger.warn("Error writing to the normalizer", e);
		}
		finally
		{
			try 
			{
				process.getProcess().getOutputStream().close();
			} 
			catch (IOException e) 
			{
			}
		}
		
		// Wait for the output parser
		try
		{
			parserThread.join();
		}
		catch (InterruptedException e)
		{
			
		}
		
		return mergeNormalisedSystemChangeScoresIntoBuckets(
				resultsParser.getNormalisedResults(), buckets);
	}
	
	
	/**
	 * Normalise the bucket and it's nested records for unusual 
	 * behaviour.
	 * The bucket's anomaly score is set to the max record score.
	 * 
	 * @param bucketSpan If <code>null</code> the default is used
	 * @param expandedBuckets Will be modified to have the normalised result
	 * @return
	 * @throws NativeProcessRunException
	 */
	public List<Bucket> normaliseForUnusualBehaviour(Integer bucketSpan, 
			List<Bucket> expandedBuckets) throws NativeProcessRunException 
	{
		InitialState state = m_JobDetailsProvider.getUnusualBehaviourInitialiser(m_JobId);
		
		NormaliserProcess process = createNormaliserProcess(
				null, state, bucketSpan);
		
		NormalisedResultsParser resultsParser = new NormalisedResultsParser(
							process.getProcess().getInputStream(),
							m_Logger);
		
		Thread parserThread = new Thread(resultsParser, m_JobId + "-Normalizer-Parser");
		parserThread.start();
						
		LengthEncodedWriter writer = new LengthEncodedWriter(
				process.getProcess().getOutputStream());
		
		
		try 
		{
			writer.writeNumFields(2);
			writer.writeField("probability");
			writer.writeField("tag");
			
			for (Bucket bucket : expandedBuckets)
			{
				for (AnomalyRecord record : bucket.getRecords())
				{
					if (record.isSimpleCount())
					{
						continue;
					}
									
					writer.writeNumFields(2);
					writer.writeField(Double.toString(record.getProbability()));
					writer.writeField(distingusherString(record));
				}
			}
		}
		catch (IOException e) 
		{
			m_Logger.warn("Error writing to the normalizer", e);
		}
		finally
		{
			try 
			{
				process.getProcess().getOutputStream().close();
			} 
			catch (IOException e) 
			{
			}
		}
		
		// Wait for the output parser
		try
		{
			parserThread.join();
		}
		catch (InterruptedException e)
		{
			
		}
		
		return mergeNormalisedUnusualIntoBuckets(
				resultsParser.getNormalisedResults(), expandedBuckets);	
	}
	
	
	
	/**
	 * For each record set the normalised value by unusual behaviour
	 * and the system change anomaly score to the buckets system
	 * change score 
	 * 
	 * @param bucketSpan If <code>null</code> the default is used
	 * @param buckets Required for normalising by system state
	 * change
	 * @param records 
	 * @param normType
	 * @return
	 * @throws NativeProcessRunException
	 */
	public List<AnomalyRecord> normaliseForBoth(Integer bucketSpan, 
			List<Bucket> buckets, List<AnomalyRecord> records, 
			NormalizationType normType) 
	throws NativeProcessRunException
	{
		InitialState sysChangeState = m_JobDetailsProvider.getSystemChangeInitialiser(m_JobId);
		InitialState unusualBehaviourState = m_JobDetailsProvider.getUnusualBehaviourInitialiser(m_JobId);
		
		
		NormaliserProcess process = createNormaliserProcess(
				sysChangeState, unusualBehaviourState, bucketSpan);
		
		NormalisedResultsParser resultsParser = new NormalisedResultsParser(
							process.getProcess().getInputStream(),
							m_Logger);

		Thread parserThread = new Thread(resultsParser, m_JobId + "-Normalizer-Parser");
		parserThread.start();

		LengthEncodedWriter writer = new LengthEncodedWriter(
				process.getProcess().getOutputStream());
		try 
		{
			writer.writeNumFields(2);
			writer.writeField(ProcessCtrl.PROBABILITY);
			writer.writeField(ProcessCtrl.RAW_ANOMALY_SCORE);
			
			// normalise the buckets first
			if (normType == NormalizationType.STATE_CHANGE || 
					normType == NormalizationType.BOTH)
			{
				for (Bucket bucket : buckets)
				{
					writer.writeNumFields(2);
					writer.writeField("");
					writer.writeField(Double.toString(bucket.getAnomalyScore()));
				}
			}
			
			if (normType == NormalizationType.UNUSUAL_BEHAVIOUR || 
					normType == NormalizationType.BOTH)
			{
				for (AnomalyRecord record : records)
				{
					if (record.isSimpleCount() != null && record.isSimpleCount())
					{
						continue;
					}
	
					writer.writeNumFields(2);
					writer.writeField(Double.toString(record.getProbability()));
					writer.writeField("");
				}
			}
		}
		catch (IOException e) 
		{
			m_Logger.warn("Error writing to the normalizer", e);
		}
		finally
		{
			try 
			{   
				// closing the input to the job terminates it 
				process.getProcess().getOutputStream().close();
			} 
			catch (IOException e) 
			{
			}
		}

		// Wait for the output parser
		try
		{
			parserThread.join();
		}
		catch (InterruptedException e)
		{
		}

		return mergeBothScoresIntoBuckets(
				resultsParser.getNormalisedResults(), buckets, records,
				normType);	
	}
	
	
	/**
	 * Replace bucket anomaly scores and scale the individual
	 * record's scores by the normalisation factor
	 * 
	 * @param normalisedScores
	 * @param buckets
	 * @return
	 */
	private List<Bucket> mergeNormalisedSystemChangeScoresIntoBuckets(
			List<NormalisedResult> normalisedScores,
			List<Bucket> buckets)
	{
		Iterator<Bucket> bucketIter = buckets.iterator();
		
		for (NormalisedResult result : normalisedScores)
		{
			Bucket bucket = bucketIter.next();
			bucket.setAnomalyScore(result.getNormalizedSysChangeScore());
		}
		
		return buckets;
	}
	
	
	/**
	 * Set the bucket's anomaly score equal to the sum of the 
	 * normalised records
	 * 
	 * @param normalisedScores
	 * @param buckets
	 * @return
	 */
	private List<Bucket> mergeNormalisedUnusualIntoBuckets(
			List<NormalisedResult> normalisedScores,
			List<Bucket> buckets)
	{
		Iterator<NormalisedResult> scoresIter = normalisedScores.iterator();
		
		for (Bucket bucket : buckets)
		{
			double bucketAnomalyScore = 0.0;
			for (AnomalyRecord record : bucket.getRecords())
			{
				if (record.isSimpleCount() != null && record.isSimpleCount())
				{
					continue;
				}

				NormalisedResult normalised = scoresIter.next();

				record.setUnusualScore(normalised.getNormalizedUnusualScore());
				
				bucketAnomalyScore = Math.max(bucketAnomalyScore,
						normalised.getNormalizedUnusualScore());
			}

			bucket.setAnomalyScore(bucketAnomalyScore);
		}
				
		return buckets;
	}
	
	
	private List<AnomalyRecord> mergeBothScoresIntoBuckets(
			List<NormalisedResult> normalisedScores,
			List<Bucket> buckets,
			List<AnomalyRecord> records, NormalizationType normType)
	{
		Iterator<NormalisedResult> scoresIter = normalisedScores.iterator();
		
		Map<String, Double> bucketIdToScore = new HashMap<>();

		// bucket sys change score first
		if (normType == NormalizationType.STATE_CHANGE || 
				normType == NormalizationType.BOTH)
		{
			for (Bucket bucket : buckets)
			{
				NormalisedResult normalised = scoresIter.next();
				bucketIdToScore.put(bucket.getId(), normalised.getNormalizedSysChangeScore());
			}
		}
		
		// set scores for records
		if (normType == NormalizationType.UNUSUAL_BEHAVIOUR) 
		{
			for (AnomalyRecord record : records)
			{
				NormalisedResult normalised = scoresIter.next();
				record.setUnusualScore(normalised.getNormalizedUnusualScore());
			}
		}
		else if (normType == NormalizationType.STATE_CHANGE)
		{
			for (AnomalyRecord record : records)
			{
				Double anomalyScore = bucketIdToScore.get(record.getParent());
				record.setAnomalyScore(anomalyScore);
			}
		}
		else 
		{
			for (AnomalyRecord record : records)
			{
				Double anomalyScore = bucketIdToScore.get(record.getParent());
				record.setAnomalyScore(anomalyScore);

				NormalisedResult normalised = scoresIter.next();
				record.setUnusualScore(normalised.getNormalizedUnusualScore());
			}
		}
				
		return records;
	}
	
	
	/***
	 * Create and start the normalization process
	 * 
	 * @param type
	 * @param state
	 * @param bucketSpan If <code>null</code> the default is used
	 * @return
	 * @throws NativeProcessRunException
	 */
	private NormaliserProcess createNormaliserProcess(
			InitialState sysChangeState, 
			InitialState unusualBehaviourState, 
			Integer bucketSpan)
	throws NativeProcessRunException
	{
		try
		{
			Process proc = ProcessCtrl.buildNormaliser(m_JobId, 
					sysChangeState, unusualBehaviourState,
					bucketSpan,  m_Logger);
			
			return new NormaliserProcess(proc);
		}
		catch (IOException e)
		{
			String msg = "Failed to start normalisation process for job " + m_JobId;
			m_Logger.error(msg, e);
			throw new NativeProcessRunException(msg, 
					ErrorCode.NATIVE_PROCESS_START_ERROR, e);
		}
	}	
	
	
	private String distingusherString(AnomalyRecord record)
	{
		StringBuilder distinguisher = new StringBuilder();
		String field = record.getByFieldValue();
		distinguisher.append(field == null ? "" : field );
		field = record.getByFieldName();
		distinguisher.append(field == null ? "" : field );
		field = record.getOverFieldValue();
		distinguisher.append(field == null ? "" : field );
		field = record.getOverFieldName();
		distinguisher.append(field == null ? "" : field );
		field = record.getPartitionFieldValue();
		distinguisher.append(field == null ? "" : field );
		field = record.getPartitionFieldName();
		distinguisher.append(field == null ? "" : field );
		
		return distinguisher.toString();
	}
}
