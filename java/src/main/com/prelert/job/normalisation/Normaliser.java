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
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

import com.prelert.job.QuantilesState;
import com.prelert.job.UnknownJobException;
import com.prelert.job.input.LengthEncodedWriter;
import com.prelert.job.persistence.JobProvider;
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
	private String m_JobId;

	private JobProvider m_JobProvider;

	private Logger m_Logger; 
	
	public Normaliser(String jobId, JobProvider jobProvider,
			Logger logger)
	{
		m_JobId = jobId;
		m_JobProvider = jobProvider;
		m_Logger = logger;
	}
			
	/**	 
	 * Normalise buckets anomaly score for system state change.
	 * 
	 * @param bucketSpan If <code>null</code> the default is used
	 * @param buckets Will be modified to have the normalised result
	 * @return
	 * @throws NativeProcessRunException
	 * @throws UnknownJobException
	 */
	public List<Bucket> normaliseForSystemChange(Integer bucketSpan, 
			List<Bucket> buckets) 
	throws NativeProcessRunException, UnknownJobException
	{
		QuantilesState state = m_JobProvider.getQuantilesState(m_JobId);
		
		NormaliserProcess process = createNormaliserProcess(
				state.getQuantilesState(QuantilesState.SYS_CHANGE_QUANTILES_KIND), null, bucketSpan);
		
		long start = System.currentTimeMillis();
		
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
			writer.writeField("rawAnomalyScore");
			writer.writeField("id");
			
			for (Bucket bucket : buckets)
			{
				writer.writeNumFields(2);
				writer.writeField(Double.toString(bucket.getRawAnomalyScore()));
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
		
		List<Bucket> result = mergeNormalisedSystemChangeScoresIntoBuckets(
				resultsParser.getNormalisedResults(), buckets);
		
		
		System.out.println("Normalise for Sys Change in : " +
				(System.currentTimeMillis() - start));
		
		
		return result;
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
	 * @throws UnknownJobException
	 */
	public List<Bucket> normaliseForUnusualBehaviour(Integer bucketSpan, 
			List<Bucket> expandedBuckets)
	throws NativeProcessRunException, UnknownJobException
	{
		QuantilesState state = m_JobProvider.getQuantilesState(m_JobId);
		
		NormaliserProcess process = createNormaliserProcess(
				null, state.getQuantilesState(QuantilesState.UNUSUAL_QUANTILES_KIND), bucketSpan);
		
		long start = System.currentTimeMillis();
		
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
			writer.writeField("id");
			
			for (Bucket bucket : expandedBuckets)
			{
				for (AnomalyRecord record : bucket.getRecords())
				{
					writer.writeNumFields(2);
					writer.writeField(Double.toString(record.getProbability()));
					writer.writeField("TODO");
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
		
		List<Bucket> buckets = mergeNormalisedUnusualIntoBuckets(
				resultsParser.getNormalisedResults(), expandedBuckets);	
		
		System.out.println("Normalise for unusual in : " +
				(System.currentTimeMillis() - start));
		
		return buckets;
	}
		
	
	/**
	 * For each record set the normalised value by unusual behaviour
	 * and the system change anomaly score to the buckets system
	 * change score 
	 * 
	 * @param bucketSpan If <code>null</code> the default is used
	 * @param buckets Required for normalising by system state
	 * change.
	 * @param records
	 * @return
	 * @throws NativeProcessRunException
	 * @throws UnknownJobException
	 */
	public List<AnomalyRecord> normalise(Integer bucketSpan, 
			List<Bucket> buckets, List<AnomalyRecord> records)
	throws NativeProcessRunException, UnknownJobException
	{
		String sysChangeState = null;
		String unusualBehaviourState = null;
		QuantilesState state = m_JobProvider.getQuantilesState(m_JobId);

		sysChangeState = state.getQuantilesState(QuantilesState.SYS_CHANGE_QUANTILES_KIND);
		unusualBehaviourState = state.getQuantilesState(QuantilesState.UNUSUAL_QUANTILES_KIND);
		
		long startProc = System.currentTimeMillis();
		NormaliserProcess process = createNormaliserProcess(
				sysChangeState, unusualBehaviourState, bucketSpan);
		
		System.out.println("Proc created in : " +
				(System.currentTimeMillis() - startProc));
		
		long start = System.currentTimeMillis();
		
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
			for (Bucket bucket : buckets)
			{
				writer.writeNumFields(2);
				writer.writeField("");
				writer.writeField(Double.toString(bucket.getRawAnomalyScore()));
			}
			
			for (AnomalyRecord record : records)
			{
				writer.writeNumFields(2);
				writer.writeField(Double.toString(record.getProbability()));
				writer.writeField("");
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

		List<AnomalyRecord> result = mergeBothScoresIntoRecords(
				resultsParser.getNormalisedResults(), buckets, records);
		
		System.out.println("Normalise for both in : " +
				(System.currentTimeMillis() - start));
		
		return result;
		
	}
	
	
	/**
	 * Replace bucket and record anomaly scores with the new
	 * normalised scores
	 * 
	 * @param normalisedScores
	 * @param buckets
	 * @return
	 * @throws NativeProcessRunException 
	 */
	private List<Bucket> mergeNormalisedSystemChangeScoresIntoBuckets(
			List<NormalisedResult> normalisedScores,
			List<Bucket> buckets) 
	throws NativeProcessRunException
	{
		Iterator<NormalisedResult> resultIter = normalisedScores.iterator();
		
		try
		{
			for (Bucket bucket : buckets)
			{
				double anomalyScore = resultIter.next().getNormalizedSysChangeScore();
				bucket.setAnomalyScore(anomalyScore);
				
				for (AnomalyRecord record : bucket.getRecords())
				{
					record.setAnomalyScore(anomalyScore);
				}
			}
		}
		catch (NoSuchElementException e)
		{
			String msg = "Error iterating normalised results";
			m_Logger.error(msg, e);
			throw new NativeProcessRunException(msg, ErrorCode.NATIVE_PROCESS_ERROR);
		}
		
		return buckets;
	}
	
	
	/**
	 * Set the bucket's unusual score equal to the highest of those
	 * on the records it contains
	 * 
	 * @param normalisedScores
	 * @param buckets
	 * @return
	 * @throws NativeProcessRunException 
	 */
	private List<Bucket> mergeNormalisedUnusualIntoBuckets(
			List<NormalisedResult> normalisedScores,
			List<Bucket> buckets) throws NativeProcessRunException
	{
		Iterator<NormalisedResult> scoresIter = normalisedScores.iterator();

		try
		{
			for (Bucket bucket : buckets)
			{
				double bucketUnusualScore = 0.0;
				for (AnomalyRecord record : bucket.getRecords())
				{
					NormalisedResult normalised = scoresIter.next();

					record.setUnusualScore(normalised.getNormalizedUnusualScore());

					bucketUnusualScore = Math.max(bucketUnusualScore,
							normalised.getNormalizedUnusualScore());
				}

				bucket.setUnusualScore(bucketUnusualScore);
			}
		}
		catch (NoSuchElementException e)
		{
			String msg = "Error iterating normalised results";
			m_Logger.error(msg, e);
			throw new NativeProcessRunException(msg, ErrorCode.NATIVE_PROCESS_ERROR);
		}
				
		return buckets;
	}
	
	
	private List<AnomalyRecord> mergeBothScoresIntoRecords(
			List<NormalisedResult> normalisedScores,
			List<Bucket> buckets,
			List<AnomalyRecord> records) 
	throws NativeProcessRunException
	{
		Iterator<NormalisedResult> scoresIter = normalisedScores.iterator();
		
		Map<String, Double> bucketIdToScore = new HashMap<>();
		
		try
		{
			// bucket sys change score first
			for (Bucket bucket : buckets)
			{
				NormalisedResult normalised = scoresIter.next();
				bucketIdToScore.put(bucket.getId(), normalised.getNormalizedSysChangeScore());
			}

			// set scores for records
			for (AnomalyRecord record : records)
			{
				Double anomalyScore = bucketIdToScore.get(record.getParent());
				record.setAnomalyScore(anomalyScore);

				NormalisedResult normalised = scoresIter.next();
				record.setUnusualScore(normalised.getNormalizedUnusualScore());
			}
		}
		catch (NoSuchElementException e)
		{
			String msg = "Error iterating normalised results";
			m_Logger.error(msg, e);
			throw new NativeProcessRunException(msg, ErrorCode.NATIVE_PROCESS_ERROR);
		}
				
		return records;
	}
	
	
	/***
	 * Create and start the normalization process
	 * 
	 * @param sysChangeState 
	 * @param unusualBehaviourState  
	 * @param bucketSpan If <code>null</code> the default is used
	 * @return
	 * @throws NativeProcessRunException
	 */
	private NormaliserProcess createNormaliserProcess(
			String sysChangeState, 
			String unusualBehaviourState, 
			Integer bucketSpan)
	throws NativeProcessRunException
	{
		long startMs = System.currentTimeMillis();
		try
		{
			Process proc = ProcessCtrl.buildNormaliser(m_JobId, 
					sysChangeState, unusualBehaviourState,
					bucketSpan,  m_Logger);

			System.out.println("Initial state written and proc created in " + 
					(System.currentTimeMillis() - startMs));
			
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
}
