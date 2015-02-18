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

package com.prelert.rs.client;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;

import org.apache.http.client.ClientProtocolException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.Detector;
import com.prelert.job.JobDetails;
import com.prelert.job.Transform;
import com.prelert.rs.data.SingleDocument;


/**
 * Create a job with a transform and test the transform is applied.
 *
 */
public class TransformJobTest implements Closeable
{
	private static final Logger LOGGER = Logger.getLogger(JobsTest.class);

	/**
	 * The default base Url used in the test
	 */
	public static final String API_BASE_URL = "http://localhost:8080/engine/v1";

	private EngineApiClient m_WebServiceClient;

	/**
	 * Creates a new http client call {@linkplain #close()} once finished
	 */
	public TransformJobTest()
	{
		m_WebServiceClient = new EngineApiClient();
	}

	@Override
	public void close() throws IOException
	{
		m_WebServiceClient.close();
	}

	public void createJob(String baseUrl) throws ClientProtocolException, IOException
	{
		final String TRANSFORM_JOB_CONFIG = "{\"id\":\"transform-job-test\","
				+ "\"description\":\"Transform Job\","
				+ "\"analysisConfig\" : {"
				+ "\"detectors\":[{\"fieldName\":\"responsetime\",\"byFieldName\":\"airline\"}]},"
				+ "\"transforms\":[{\"transform\":\"domain_lookup\", \"inputs\":[\"field1\"]}],"
				+ "\"dataDescription\":{\"fieldDelimiter\":\",\", \"timeField\":\"time\", "
				+ "\"timeFormat\":\"yyyy-MM-dd HH:mm:ssX\"} }}";

		m_WebServiceClient.deleteJob(baseUrl, "transform-job-test");

		String jobId = m_WebServiceClient.createJob(baseUrl, TRANSFORM_JOB_CONFIG);
		if (jobId == null || jobId.isEmpty())
		{
			LOGGER.error(m_WebServiceClient.getLastError().toJson());
			LOGGER.error("No Job Id returned by create job");
			test(jobId != null);
		}
		test(jobId.equals("transform-job-test"));

		// get job by location, verify
		SingleDocument<JobDetails> doc = m_WebServiceClient.getJob(baseUrl, jobId);
		if (doc.isExists() == false)
		{
			LOGGER.error("No Job at URL " + jobId);
		}
		JobDetails job = doc.getDocument();


		Detector d = new Detector();
		d.setFieldName("responsetime");
		d.setByFieldName("airline");

		AnalysisConfig ac = new AnalysisConfig();
		ac.setDetectors(Arrays.asList(d));

		DataDescription dd = new DataDescription();
		dd.setFieldDelimiter(',');
		dd.setTimeFormat("yyyy-MM-dd HH:mm:ssX");
		dd.setTimeField("time");

		test(job.getDescription().equals("Transform Job"));

		test(ac.equals(job.getAnalysisConfig()));
		test(dd.equals(job.getDataDescription()));


		Transform tr = new Transform();
		tr.setTransform("domain_lookup");
		tr.setInputs(Arrays.asList("field1"));

		test(job.getTransforms().size() == 1);
		test(tr.equals(job.getTransforms().get(0)));
	}

	/**
	 * Throws an exception if <code>condition</code> is false.
	 *
	 * @param condition
	 * @throws IllegalStateException
	 */
	public static void test(boolean condition)
	throws IllegalStateException
	{
		if (condition == false)
		{
			throw new IllegalStateException();
		}
	}

	public static void main(String[] args) throws ClientProtocolException, IOException
	{
		// configure log4j
		ConsoleAppender console = new ConsoleAppender();
		console.setLayout(new PatternLayout("%d [%p|%c|%C{1}] %m%n"));
		console.setThreshold(Level.INFO);
		console.activateOptions();
		Logger.getRootLogger().addAppender(console);

		String baseUrl = API_BASE_URL;
		if (args.length > 0)
		{
			baseUrl = args[0];
		}

		LOGGER.info("Testing Service at " + baseUrl);


		try (TransformJobTest transformTest = new TransformJobTest())
		{
			transformTest.createJob(baseUrl);
		}


		LOGGER.info("All tests passed Ok");
	}

}
