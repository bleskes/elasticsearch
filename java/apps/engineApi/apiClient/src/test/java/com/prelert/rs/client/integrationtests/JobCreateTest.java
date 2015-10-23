/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
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
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
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
package com.prelert.rs.client.integrationtests;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.Detector;
import com.prelert.job.JobConfiguration;
import com.prelert.rs.client.EngineApiClient;
import com.prelert.rs.data.ApiError;

/**
 * This tests creates a new job then lists all the jobs straight
 * afterwards. The aim flush out any errors creating new jobs and
 * doing the sorted search straight after.
 * See Bug 562.
 */
public class JobCreateTest
{
	private static final Logger LOGGER = Logger.getLogger(JobCreateTest.class);

	/**
	 * The Engine API base Url
	 */
	private static String s_ApiBaseUrl;

	/**
	 * Runnable class to create the new jobs
	 */
	private static class JobCreator implements Runnable
	{
		private int m_NumJobs;
		private JobConfiguration m_JobConfig;
		private List<String> m_JobIds;

		/**
		 *
		 * @param numJobs The number of jobs to create
		 */
		public JobCreator(int numJobs)
		{
			m_NumJobs = numJobs;

			Detector d = new Detector();
			d.setFieldName("hitcount");
			d.setByFieldName("url");
			AnalysisConfig ac = new AnalysisConfig();
			ac.setBucketSpan(86400L);
			ac.setDetectors(Arrays.asList(d));

			DataDescription dd = new DataDescription();
			dd.setFieldDelimiter('\t');

			m_JobConfig = new JobConfiguration(ac);

			m_JobIds = new ArrayList<>();
		}


		/**
		 * Create <code>m_NumJobs</code> in quick succession and get
		 * the list of all jobs after each new job is created
		 *
		 * If there is an error an  IllegalStateException is thrown.
		 */
		@Override
		public void run()
		{
			try (EngineApiClient client = new EngineApiClient(s_ApiBaseUrl))
			{
				for (int i=0; i<m_NumJobs; i++)
				{
					String jobId = client.createJob(m_JobConfig);
					if (jobId == null || jobId.isEmpty())
					{
						ApiError error = client.getLastError();
						throw new IllegalStateException("Error creating job\n" + error.toJson());
					}
					m_JobIds.add(jobId);

					//if (i % 5 == 0)
					{
						client.getJobs();
						ApiError error = client.getLastError();
						if (error != null)
						{
							throw new IllegalStateException(error.toJson());
						}
					}
				}
			}
			catch (IOException e)
			{
				throw new IllegalStateException(e);
			}
		}
	}


	public static void main(String[] args)
	throws FileNotFoundException, IOException
	{
		// configure log4j
		ConsoleAppender console = new ConsoleAppender();
		console.setLayout(new PatternLayout("%d [%p|%c|%C{1}] %m%n"));
		console.setThreshold(Level.INFO);
		console.activateOptions();
		Logger.getRootLogger().addAppender(console);

		s_ApiBaseUrl = "http://localhost:8080/engine/v1";
		if (args.length > 0)
		{
	        s_ApiBaseUrl = args[0];
		}

		LOGGER.info("Using " + s_ApiBaseUrl + " for tests");

		final int NUM_CLIENTS = 1;
		final int NUM_JOBS = 50;

		List<Thread> threads = new ArrayList<>();
		List<JobCreator> jobCreators = new ArrayList<>();
		for (int i=0; i<NUM_CLIENTS; i++)
		{
			JobCreator test = new JobCreator(NUM_JOBS);
			jobCreators.add(test);

			Thread testThread = new Thread(test);
			threads.add(testThread);
		}

		for (Thread th : threads)
		{
			th.start();
		}


		for (Thread th : threads)
		{
			try
			{
				th.join();
			}
			catch (InterruptedException e)
			{
				LOGGER.error("Interupted joining test thread", e);
			}
		}

		// now clean up the jobs
		try (EngineApiClient client = new EngineApiClient(s_ApiBaseUrl))
		{
			for (JobCreator creator : jobCreators)
			{
				for (String jobId : creator.m_JobIds)
				{
					client.deleteJob(jobId);
					ApiError error = client.getLastError();
					if (error != null)
					{
						LOGGER.error(error.toJson());
					}
				}
			}
		}

	}
}
