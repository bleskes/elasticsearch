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
import java.util.List;

import org.apache.log4j.Logger;

import com.prelert.job.input.LengthEncodedWriter;
import com.prelert.job.persistence.JobResultsProvider;
import com.prelert.job.process.NativeProcessRunException;
import com.prelert.job.process.ProcessCtrl;
import com.prelert.job.process.output.NormalisedResultsParser;
import com.prelert.rs.data.ErrorCode;

public class Normaliser 
{
	static public final Logger s_Logger = Logger.getLogger(Normaliser.class);
	
	private JobResultsProvider m_JobDetailsProvider;
	
	private Logger m_Logger;
	private String m_JobId;
	
	public Normaliser(String jobId, JobResultsProvider jobResultsProvider, Logger logger)
	{
		m_JobDetailsProvider = jobResultsProvider;
		m_JobId = jobId;
		m_Logger = logger;
	}
			
	public List<NormalisedResult> normalise() throws NativeProcessRunException
	{
		NormaliserProcess process = createNormaliserProcess();
		
		NormalisedResultsParser resultsParser = new NormalisedResultsParser(
							process.getProcess().getInputStream(),
							m_Logger);
		
		Thread parserThread = new Thread(resultsParser, m_JobId + "-Results-Parser");
		parserThread.start();
		
		List<JobResultsProvider.TimeScore> scores = m_JobDetailsProvider.getRawScores(m_JobId);
		
		LengthEncodedWriter writer = new LengthEncodedWriter(
				process.getProcess().getOutputStream());
		
		try 
		{
			writer.writeNumFields(1);
			writer.writeField("anomalyScore");
			
			for (JobResultsProvider.TimeScore score : scores)
			{
				writer.writeNumFields(1);
				writer.writeField(score.getScore());
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

		
		try
		{
			parserThread.join();
		}
		catch (InterruptedException e)
		{
			
		}
		
		
		return resultsParser.getNormalisedResults();
	}
	
	
	
	private NormaliserProcess createNormaliserProcess()
	throws NativeProcessRunException
	{
		InitialState state = new InitialState(1405074157l, 0.0001f); // TODO state hardcoded
		try
		{
			Process proc = ProcessCtrl.buildNormaliser(m_JobId, 
					ProcessCtrl.NormalisationType.SYS_STATE_CHANGE,
					state, 300,  m_Logger);
			
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
