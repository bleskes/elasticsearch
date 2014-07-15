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
import java.util.Map;

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
	public List<Map<String, Object>> normaliseForSystemChange(int bucketSpan, 
			List<Map<String, Object>> buckets) 
	throws NativeProcessRunException
	{
		ProcessCtrl.NormalisationType type = NormalisationType.SYS_STATE_CHANGE;
		InitialState state = m_JobDetailsProvider.getSystemChangeInitialiser(m_JobId);
		
		NormaliserProcess process = createNormaliserProcess(type, state, bucketSpan);
		
		NormalisedResultsParser resultsParser = new NormalisedResultsParser(
							process.getProcess().getInputStream(),
							s_Logger);
		
		Thread parserThread = new Thread(resultsParser, m_JobId + "-Results-Parser");
		parserThread.start();
						
		LengthEncodedWriter writer = new LengthEncodedWriter(
				process.getProcess().getOutputStream());
		
		try 
		{
			writer.writeNumFields(2);
			writer.writeField("anomalyScore");
			writer.writeField("tag");
			
			for (Map<String, Object> bucket : buckets)
			{
				writer.writeNumFields(2);
				writer.writeField(bucket.get(Bucket.ANOMALY_SCORE).toString());
				writer.writeField(bucket.get(Bucket.ID).toString());
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
		
		return normaliseBuckets(resultsParser.getNormalisedResults(), buckets);
	}
	
	
	private List<Map<String, Object>> normaliseBuckets(List<NormalisedResult> results,
			List<Map<String, Object>> buckets)
	{
		Iterator<Map<String, Object>> bucketIter = buckets.iterator();
		
		for (NormalisedResult result : results)
		{
			Map<String, Object> bucket = bucketIter.next();
			bucket.put(Bucket.ANOMALY_SCORE, new Double(result.getNormalizedSysChangeScore()));
			
			if (bucket.containsKey(Bucket.RECORDS))
			{
				List<Map<String, Object>> records = (List<Map<String, Object>>) 
						bucket.get(Bucket.RECORDS);
				
				for (Map<String, Object> record : records)
				{
					try
					{
						double score = Double.parseDouble(
								record.get(AnomalyRecord.ANOMALY_SCORE).toString());
						
						record.put(AnomalyRecord.ANOMALY_SCORE, 
								new Double(score * result.getSysChangeScoreMultiplier()));
								
					}
					catch (NumberFormatException nfe)
					{
						s_Logger.warn("Cannot parse record anomaly score", nfe);
					}
				}
			}
			
		}
		
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
	
}
