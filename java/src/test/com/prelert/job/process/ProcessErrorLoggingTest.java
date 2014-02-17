/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Inc 2006-2014     *
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

package com.prelert.job.process;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.DetectorState;
import com.prelert.job.JobConfiguration;
import com.prelert.job.JobDetails;
import com.prelert.job.NativeProcessRunException;
import com.prelert.job.UnknownJobException;

/**
 * Launch processes with various bad configuration/inputs that
 * should cause an error message to be written. 
 * <br/>
 * The tests require -Dprelert.home to be set
 * <br/>
 * This test isn't very successful due to timing issues with the native 
 * process and buffering of outputstreams. Even if the stream is flushed
 * it still may not be written to the process depending on what the OS is
 * doing. Even if it is written checking the process errors straight after 
 * the write finishes doesn't report an error as the process hasn't looked
 * at the data yet. For real world examples with large data files it should
 * work but when working with trivially small files it will always be an 
 * issue. The general problem is there is no way to sychronise native 
 * process and the java server. 
 */
public class ProcessErrorLoggingTest 
{
	public static final String [] CSV_LINES = new String [] {"time,airline,responsetime,sourcetype",
	                                        "2013-01-28 00:00:00,AAL,132.2046,farequote",
											"2013-01-28 00:00:00,JZA,990.4628,farequote",
											"2013-01-28 00:00:00,JBU,877.5927,farequote",
											"2013-01-28 00:00:00,KLM,1355.4812,farequote",
											"2013-01-28 00:00:00,NKS,9991.3981,farequote"};
			
	static class SingleJobDetailsProvider implements JobDetailsProvider 
	{
		private Map<String, JobDetails> m_JobDetails = new HashMap<>();
		
		public void insertJobDetails(String jobId, JobDetails details)
		{
			m_JobDetails.put(jobId, details);
		}
		
		@Override
		public JobDetails getJobDetails(String jobId) throws UnknownJobException
		{
			if (m_JobDetails.containsKey(jobId))
			{
				return m_JobDetails.get(jobId);
			}
			else 
			{
				throw new UnknownJobException(jobId, 
						"SingleJobDetailsProvider cannot find job");
			}
		}
		@Override
		public DetectorState getPersistedState(String jobId)
		{
			return null;
		}
	}
	
	
	static class DoNothingResultsPersister implements ResultsReaderFactory 
	{
		@Override
		public Runnable newResultsParser(String jobId, InputStream autoDetectOutput)
		{
			return new Runnable() {				
				@Override
				public void run() {
					return;  // do nothing					
				}
			};
		}
	}
	
	
	/**
	 * No arguments but -Dprelert.home must be set.
	 * 
	 * @param args
	 * @throws NativeProcessRunException
	 * @throws UnknownJobException
	 * @throws IOException 
	 */
	public static void main(String[] args) 
	throws NativeProcessRunException, UnknownJobException, IOException 
	{
		AnalysisConfig.Detector detector = new AnalysisConfig.Detector();
		detector.setFieldName("airline");
		detector.setByFieldName("responsetime");
		List<AnalysisConfig.Detector> d = new ArrayList<>();
		d.add(detector);
		
		AnalysisConfig conf = new AnalysisConfig();
		conf.setDetectors(d);

		DataDescription dd = new DataDescription();
		dd.setFieldDelimiter("\t");
		
		String jobId = "TestJob";
		JobDetails job = new JobDetails(jobId, 
					new JobConfiguration.JobConfigurationBuilder(conf)
				.dataDescription(dd)
				.build());
		
		SingleJobDetailsProvider jobDetailsProvider = new SingleJobDetailsProvider();
		jobDetailsProvider.insertJobDetails(jobId, job);
		
		StringBuilder builder = new StringBuilder();
		for (String line : CSV_LINES)
		{
			builder.append(line).append('\n');
		}		
		ByteArrayInputStream bis = new ByteArrayInputStream(builder.toString().getBytes(Charset.forName("UTF-8")));

		
		
		ProcessManager manager = new ProcessManager(jobDetailsProvider,
										new DoNothingResultsPersister());
		try
		{

			try 
			{
				manager.dataToJob("unknown_job", bis);
				throw new IllegalStateException("Creating job with unknow id should have "
						+ "thrown a UnknownJobException");
			}
			catch (UnknownJobException e) 
			{
				// this is expected
			} 


			try
			{			
				manager.dataToJob(jobId, bis);
//				throw new IllegalStateException("Writing data to job in wrong format "
//						+ "should throw a NativeProcessRunException");
				
				bis.close();
			}
			catch (NativeProcessRunException e)
			{
				// we expect this because the time field has the wrong name
			}

			manager.finishJob(jobId);
		}
		finally
		{
			manager.stop();
		}
	}

}
