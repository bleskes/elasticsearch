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

package com.prelert.rs.client;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.Detector;
import com.prelert.job.JobConfiguration;
import com.prelert.job.alert.Alert;
import com.prelert.rs.data.AnomalyRecord;
import com.prelert.rs.data.ApiError;
import com.prelert.rs.data.Bucket;
import com.prelert.rs.data.ErrorCode;


/**
 * Tests the long poll alert subscription end point.
 * <br>Jobs are created and an alert is subscribed for each then data
 * is uploaded to the job. Some alerts are expected to fire others
 * will timout.
 * <br>Returns a non-zero value if the tests fail.
 */
public class AlertSubscriptionTest
{
	private final static Logger s_Logger = Logger.getLogger(AlertSubscriptionTest.class);

	/**
	 * The default base Url used in the test
	 */
	static final public String API_BASE_URL = "http://localhost:8080/engine/v1";

	static final public String ALERTING_JOB_1 = "alerting-job-1";
	static final public String ALERTING_JOB_2 = "alerting-job-2";
	static final public String ALERTING_JOB_3 = "alerting-job-3";
	static final public String ALERTING_JOB_4 = "alerting-job-4";

	static final public String [] JOB_IDS = {ALERTING_JOB_1, ALERTING_JOB_2, ALERTING_JOB_3, ALERTING_JOB_4};


	static private void setupJobs(String baseUrl, EngineApiClient client)
	throws ClientProtocolException, IOException
	{
		for (String s : JOB_IDS)
		{
			client.deleteJob(baseUrl, s);
		}

		Detector d1 = new Detector();
		d1.setFieldName("In Discards");
		d1.setByFieldName("host");
		Detector d2 = new Detector();
		d2.setFieldName("In Octets");
		d2.setByFieldName("host");
		Detector d3 = new Detector();
		d3.setFieldName("Out Discards");
		d3.setByFieldName("host");
		Detector d4 = new Detector();
		d4.setFieldName("Out Octets");
		d4.setByFieldName("host");

		AnalysisConfig ac = new AnalysisConfig();
		ac.setBucketSpan(3600L);
		ac.setDetectors(Arrays.asList(d1, d2, d3, d4));

		DataDescription dd = new DataDescription();
		dd.setFieldDelimiter(',');
		dd.setTimeField("time");
		dd.setTimeFormat("EEE MM/dd/yyyy HH:mm");

		JobConfiguration config = new JobConfiguration();
		config.setAnalysisConfig(ac);
		config.setDataDescription(dd);

		for (String s : JOB_IDS)
		{
			config.setId(s);
			client.createJob(baseUrl, config);
		}
	}

	static private void configureLogging()
	{
		// configure log4j
		ConsoleAppender console = new ConsoleAppender();
		console.setLayout(new PatternLayout("%d [%p|%c|%C{1}] %m%n"));
		console.setThreshold(Level.INFO);
		console.activateOptions();
		Logger.getRootLogger().addAppender(console);
	}


	/**
	 * Throws if <code>condition</code> if false
	 *
	 * @param condition
	 * @throws IllegalStateException
	 */
	static public void test(boolean condition)
	throws IllegalStateException
	{
		if (condition == false)
		{
			throw new IllegalStateException();
		}
	}

	public static void main(String[] args)
	throws FileNotFoundException, IOException, InterruptedException
	{
		configureLogging();

		String baseUrl = API_BASE_URL;
		if (args.length > 0)
		{
			baseUrl = args[0];
		}

		s_Logger.info("Testing Service at " + baseUrl);

		final String prelertTestDataHome = System.getProperty("prelert.test.data.home");
		if (prelertTestDataHome == null)
		{
			s_Logger.error("Error property prelert.test.data.home is not set");
			return;
		}

		EngineApiClient client = new EngineApiClient();
		setupJobs(baseUrl, client);

		Alert alert = client.pollJobAlert(baseUrl, "non-existing-job", 5, null, null);
		test(alert == null);
		ApiError error = client.getLastError();
		test(error != null);
		test(error.getErrorCode() == ErrorCode.MISSING_JOB_ERROR);

		alert = client.pollJobAlert(baseUrl, ALERTING_JOB_1, 5, null, null);
		test(alert == null);
		error = client.getLastError();
		test(error != null);
		test(error.getErrorCode() == ErrorCode.JOB_NOT_RUNNING);


		List<Thread> uploaderThreads = new ArrayList<>();
		List<LongPollAlertTest> longPollTests = new ArrayList<>();
		List<Thread> alertTestThreads = new ArrayList<>();

		for (String jobId : JOB_IDS)
		{
			File networkDataFile = new File(prelertTestDataHome +
					"/engine_api_integration_test/network.csv");

			DataUploader dl = new DataUploader(new EngineApiClient(), jobId, baseUrl, networkDataFile);
			dl.initiateUpload(); // makes the job active
			uploaderThreads.add(new Thread(dl));

			LongPollAlertTest longPoll;
			if (jobId == ALERTING_JOB_4)
			{
				// this one will timeout
				longPoll = new LongPollAlertTest(client, baseUrl, jobId, 99.9, 99.9, true);
			}
			else if (jobId == ALERTING_JOB_3)
			{
				// this one will timeout
				longPoll = new LongPollAlertTest(client, baseUrl, jobId, null, 5.0, false);
			}
			else if (jobId == ALERTING_JOB_2)
			{
				// this one will timeout
				longPoll = new LongPollAlertTest(client, baseUrl, jobId, 7.0, null, false);
			}
			else
			{
				// have 2 alerters for this job
				longPoll = new LongPollAlertTest(client, baseUrl, jobId, 14.0, 2.3, false);
				Thread th = new Thread(longPoll);
				alertTestThreads.add(th);
				th.start();

				longPoll = new LongPollAlertTest(client, baseUrl, jobId, 4.5, 2.3, false);
				longPollTests.add(longPoll);
			}

			longPollTests.add(longPoll);
			Thread th = new Thread(longPoll);
			alertTestThreads.add(th);
			th.start();
		}

		s_Logger.info("Starting upload threads");
		for (Thread th : uploaderThreads)
		{
			th.start();
		}

		for (Thread th : uploaderThreads)
		{
			th.join();
		}

		s_Logger.info("Upload threads finished");

		// if alerting threads haven't stopped now they never will
		for (LongPollAlertTest test : longPollTests)
		{
			test.quit();
		}

		for (Thread th : alertTestThreads)
		{
			th.join();
		}

		boolean passed = true;
		for (LongPollAlertTest test : longPollTests)
		{
			passed = passed && test.isTestPassed();
		}

		if (passed)
		{
			s_Logger.info("All alert tests passed");
		}
		else
		{
			s_Logger.info("Alert tests failed");
			System.exit(1);
		}

	}


	/**
	 * Subscribe to the long poll alerts end point for the job.
	 * When the alert is fired chech its values and if it timed
	 * out or not.
	 */
	static private class LongPollAlertTest implements Runnable
	{
		EngineApiClient m_Client;
		String m_BaseUrl;
		String m_JobId;
		Double m_ScoreThreshold;
		Double m_ProbabiltyThreshold;
		boolean m_ShouldTimeout;
		volatile boolean m_Quit;
		boolean m_TestPassed;


		public LongPollAlertTest(EngineApiClient client, String baseUrl, String jobId,
				Double scoreThreshold, Double probabiltyThreshold, boolean shouldTimeout)
		{
			m_Client = client;
			m_BaseUrl = baseUrl;
			m_JobId = jobId;
			m_ScoreThreshold = scoreThreshold;
			m_ProbabiltyThreshold = probabiltyThreshold;
			m_ShouldTimeout = shouldTimeout;
		}

		/**
		 * Stops the thread's run loop
		 */
		public void quit()
		{
			m_Quit = true;
		}

		/**
		 * Tests passed
		 * @return
		 */
		public boolean isTestPassed()
		{
			return m_TestPassed;
		}


		@Override
		public void run()
		{
			try
			{
				final int TIMEOUT = 30;

				Alert alert = m_Client.pollJobAlert(m_BaseUrl, m_JobId, TIMEOUT,
						m_ScoreThreshold, m_ProbabiltyThreshold);

				// may get errors about the job not running if the data
				// upload hasn't started
				while (alert == null && m_Client.getLastError() != null)
				{
					ApiError err = m_Client.getLastError();
					test(err.getErrorCode() == ErrorCode.JOB_NOT_RUNNING);

					alert = m_Client.pollJobAlert(m_BaseUrl, m_JobId, TIMEOUT,
							m_ScoreThreshold, m_ProbabiltyThreshold);

					if (m_Quit)
					{
						break;
					}
				}

				// alert should exist at this point
				test(alert != null);
				test(alert.getJobId().equals(m_JobId));
				test(alert.isTimeout() == m_ShouldTimeout);
				if (alert.isTimeout() == false)
				{
					if (m_ScoreThreshold != null && m_ProbabiltyThreshold != null)
					{
						test(alert.getAnomalyScore() >= m_ScoreThreshold ||
								alert.getNormalizedProbability() >= m_ProbabiltyThreshold);
					}
					else if (m_ScoreThreshold != null)
					{
						test(alert.getAnomalyScore() >= m_ScoreThreshold);
						test(alert.getRecords() == null);
						test(alert.getBucket() != null);
						test(alert.getBucket().getAnomalyScore() >= m_ScoreThreshold);
					}
					else if (m_ProbabiltyThreshold != null)
					{
						test(alert.getNormalizedProbability() >= m_ProbabiltyThreshold);
						test(alert.getBucket() == null);
						test(alert.getRecords() != null);
						test(alert.getRecords().size() > 0);
						for (AnomalyRecord r : alert.getRecords())
						{
							test(r.getNormalizedProbability() >= m_ProbabiltyThreshold);
						}
					}
				}

				m_TestPassed = true;
			}
			catch (JsonParseException e)
			{
				e.printStackTrace();
				test(false);
			}
			catch (JsonMappingException e)
			{
				e.printStackTrace();
				test(false);
			}
			catch (IOException e)
			{
				e.printStackTrace();
				test(false);
			}
		}
	}




	/**
	 * Upload the contents of a file to the Engine API
	 * and closes the job.
	 * <br>The initiateUpload() upload function writes the csv
	 * header so the job will be started. run() then writes the
	 * rest of the file.
	 *
	 */
	static private class DataUploader implements Runnable
	{
		EngineApiClient m_Client;
		String m_BaseUrl;
		String m_JobId;
		File m_File;

		public DataUploader(EngineApiClient client, String jobId, String baseUrl, File file)
		throws IOException
		{
			m_Client = client;
			m_BaseUrl = baseUrl;
			m_JobId = jobId;
			m_File = file;
		}

		/**
		 * Uploads the beginning of the data to the job causing the job
		 * to become active
		 */
		public void initiateUpload()
		{
			try
			{
				BufferedReader reader = new BufferedReader(new FileReader(m_File));
				String header = reader.readLine();

				ByteArrayInputStream is = new ByteArrayInputStream(header.getBytes(StandardCharsets.UTF_8));
				m_Client.streamingUpload(m_BaseUrl, m_JobId, is, false);
				reader.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
				test(false);
			}
		}

		@Override
		public void run()
		{
			try
			{
				m_Client.fileUpload(m_BaseUrl, m_JobId, m_File, false);
				m_Client.closeJob(m_BaseUrl, m_JobId);
			}
			catch (IOException e)
			{
				e.printStackTrace();
				test(false);
			}
		}
	}
}
