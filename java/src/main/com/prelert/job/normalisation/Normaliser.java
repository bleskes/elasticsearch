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
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.prelert.job.input.LengthEncodedWriter;
import com.prelert.job.persistence.JobResultsProvider;
import com.prelert.job.process.NativeProcessRunException;
import com.prelert.job.process.ProcessCtrl;
import com.prelert.job.process.ProcessCtrl.NormalisationType;
import com.prelert.job.process.output.NormalisedResultsParser;
import com.prelert.rs.data.AnomalyRecord;
import com.prelert.rs.data.Bucket;
import com.prelert.rs.data.ErrorCode;


public class Normaliser 
{
	static public final Logger s_Logger = Logger.getLogger(Normaliser.class);
	
	private JobResultsProvider m_JobDetailsProvider;
	
	private String m_JobId;
	
	private int m_Written;
	
	public Normaliser(String jobId, JobResultsProvider jobResultsProvider)
	{
		m_JobDetailsProvider = jobResultsProvider;
		m_JobId = jobId;
	}
			
	/**	 
	 *  
	 * @param bucketSpan
	 * @return
	 * @throws NativeProcessRunException
	 */
	public List<Bucket> normaliseForSystemChange(int bucketSpan, 
			List<Bucket> buckets) 
	throws NativeProcessRunException
	{
		InitialState state = m_JobDetailsProvider.getSystemChangeInitialiser(m_JobId);
		
		NormaliserProcess process = createNormaliserProcess(
				NormalisationType.SYS_STATE_CHANGE, state, bucketSpan);
		
		NormalisedResultsParser resultsParser = new NormalisedResultsParser(
							process.getProcess().getInputStream(),
							s_Logger);
		
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
			s_Logger.warn("Error writing to the normalizer", e);
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
	
	
	public List<Bucket> normaliseForUnusualBehaviour(int bucketSpan, 
			List<Bucket> expandedBuckets) throws NativeProcessRunException 
	{
		InitialState state = m_JobDetailsProvider.getUnusualBehaviourInitialiser(m_JobId);
		
		NormaliserProcess process = createNormaliserProcess(
				NormalisationType.UNUSUAL_STATE, state, bucketSpan);
		
		NormalisedResultsParser resultsParser = new NormalisedResultsParser(
							process.getProcess().getInputStream(),
							s_Logger);
		
		Thread parserThread = new Thread(resultsParser, m_JobId + "-Normalizer-Parser");
		parserThread.start();
						
		LengthEncodedWriter writer = new LengthEncodedWriter(
				process.getProcess().getOutputStream());
		
		
		try 
		{
			writer.writeNumFields(2);
			writer.writeField("probability");
			writer.writeField("tag");
			
			System.out.println("probability,tag");
			
			
			m_Written = 0;
			for (Bucket bucket : expandedBuckets)
			{
				for (AnomalyRecord record : bucket.getRecords())
				{
					if (record.isSimpleCount() != null && record.isSimpleCount())
					{
						continue;
					}
					
					System.out.println(String.format("%s,%s", 
							Double.toString(record.getProbability()),
							distingusherString(record)));	
					
					writer.writeNumFields(2);
					writer.writeField(Double.toString(record.getProbability()));
					writer.writeField(distingusherString(record));
					
					m_Written++;
				}
			}
			
			System.out.println(m_Written);
		}
		catch (IOException e) 
		{
			s_Logger.warn("Error writing to the normalizer", e);
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
				m_Written--;
				NormalisedResult normalised = scoresIter.next();

				bucketAnomalyScore += normalised.getNormalizedUnusualScore();
			}

			bucket.setAnomalyScore(bucketAnomalyScore);
		}
		
		System.out.println(m_Written);
		
		return buckets;
	}
	
	/***
	 * Create and start the normalization process
	 * 
	 * @param type
	 * @param state
	 * @param bucketSpan
	 * @return
	 * @throws NativeProcessRunException
	 */
	private NormaliserProcess createNormaliserProcess(
			ProcessCtrl.NormalisationType type,
			InitialState state, 
			int bucketSpan)
	throws NativeProcessRunException
	{
		try
		{
			Process proc = ProcessCtrl.buildNormaliser(m_JobId, 
					type, state, bucketSpan,  s_Logger);
			
			return new NormaliserProcess(proc);
		}
		catch (IOException e)
		{
			String msg = "Failed to start normalisation process for job " + m_JobId;
			s_Logger.error(msg, e);
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
