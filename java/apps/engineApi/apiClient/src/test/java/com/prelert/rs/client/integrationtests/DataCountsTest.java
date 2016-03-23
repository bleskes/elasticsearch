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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataCounts;
import com.prelert.job.DataDescription;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.Detector;
import com.prelert.job.JobConfiguration;
import com.prelert.job.JobDetails;
import com.prelert.rs.client.EngineApiClient;
import com.prelert.rs.data.ApiError;
import com.prelert.rs.data.MultiDataPostResult;
import com.prelert.rs.data.SingleDocument;

/**
 * Test the data counts (num records, fields, buckets, etc)
 * after uploading data to a job. Verifys both the counts
 * returned by the data upload and the counts object inside
 * the {@linkplain JobDetails} instance.
 */
public class DataCountsTest implements Closeable
{
	private static final Logger LOGGER = Logger.getLogger(DataCountsTest.class);

	/**
	 * The default base Url used in the test
	 */
	public static final String API_BASE_URL = "http://localhost:8080/engine/v2";

	static final long FLIGHTCENTRE_NUM_BUCKETS = 296;
	static final long FLIGHTCENTRE_NUM_RECORDS = 175910;
	static final long FLIGHTCENTRE_NUM_FIELDS = FLIGHTCENTRE_NUM_RECORDS * 3;
	static final long FLIGHTCENTRE_NUM_PROCESSED_FIELDS = FLIGHTCENTRE_NUM_RECORDS * 2;
	static final long FLIGHTCENTRE_INPUT_BYTES_CSV = 5724086;

	static final long FLIGHTCENTRE_INPUT_BYTES_JSON = 17510020;


	private EngineApiClient m_WebServiceClient;

	public DataCountsTest(String baseUrl)
	{
		m_WebServiceClient = new EngineApiClient(baseUrl);
	}

	@Override
	public void close() throws IOException
	{
		m_WebServiceClient.close();
	}



	private String runFarequoteJob(File dataFile, boolean isJson, boolean compressed)
	throws IOException
	{
		Detector d = new Detector();
		d.setFieldName("responsetime");
		d.setByFieldName("airline");

		AnalysisConfig ac = new AnalysisConfig();
		ac.setBucketSpan(300L);
		ac.setOverlappingBuckets(false);
		ac.setDetectors(Arrays.asList(d));

		DataDescription dd = new DataDescription();
		if (isJson)
		{
			dd.setFormat(DataFormat.JSON);
			dd.setTimeField("timestamp");
			dd.setTimeFormat("epoch");
		}
		else
		{
			dd.setFormat(DataFormat.DELIMITED);
			dd.setFieldDelimiter(',');
			dd.setTimeField("_time");
			dd.setTimeFormat("epoch");
		}


		JobConfiguration config = new JobConfiguration(ac);
		config.setDescription("Farequote usage test");
		config.setDataDescription(dd);

		String jobId = m_WebServiceClient.createJob(config);
		if (jobId == null || jobId.isEmpty())
		{
			LOGGER.error("No Job Id returned by create job");
			LOGGER.info(m_WebServiceClient.getLastError().toJson());
			test(jobId != null && jobId.isEmpty() == false);
		}

		MultiDataPostResult result = m_WebServiceClient.fileUpload(jobId, dataFile, compressed);

        test(result.anErrorOccurred() == false);
        test(result.getResponses().size() == 1);
        ApiError error = result.getResponses().get(0).getError();
        test(error == null);

		if (result.getResponses().get(0).getUploadSummary().getInputRecordCount() == 0)
		{
			LOGGER.error(error.toJson());
			test(false);
		}

		validateFlightCentreCounts(result.getResponses().get(0).getUploadSummary(),
		                        isJson, compressed, false);

		m_WebServiceClient.closeJob(jobId);


		return jobId;
	}

	private DataCounts jobDataCounts(String jobId) throws IOException
	{
	    SingleDocument<JobDetails> job =  m_WebServiceClient.getJob(jobId);
	    test(job.isExists());

	    return job.getDocument().getCounts();
	}


	public void validateFlightCentreCounts(DataCounts counts, boolean isJson, boolean isCompressed,
	                                boolean compareBucketCount)
	throws IOException
	{
	    if (compareBucketCount)
		{
	        test(counts.getBucketCount() == FLIGHTCENTRE_NUM_BUCKETS);
		}
		test(counts.getInputRecordCount() == FLIGHTCENTRE_NUM_RECORDS);
		test(counts.getInputFieldCount() == FLIGHTCENTRE_NUM_FIELDS);

		if (isJson)
		{
			test(counts.getInputBytes() == FLIGHTCENTRE_INPUT_BYTES_JSON);
		}
		else
		{
			// the gzipped data is 1 byte smaller (assuming this is a new line)
		    if (isCompressed)
			{
		        test(counts.getInputBytes() == FLIGHTCENTRE_INPUT_BYTES_CSV -1);
			}
		    else
		    {
		        test(counts.getInputBytes() == FLIGHTCENTRE_INPUT_BYTES_CSV);
		    }

		}

		test(counts.getProcessedRecordCount() == counts.getInputRecordCount());
		test(counts.getProcessedFieldCount() == FLIGHTCENTRE_NUM_PROCESSED_FIELDS);

		test(counts.getInvalidDateCount() == 0);
		test(counts.getMissingFieldCount() == 0);
		test(counts.getOutOfOrderTimeStampCount() == 0);
	}

	/**
	 * Throws an IllegalStateException if <code>condition</code> is false.
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

	public static void main(String[] args)
	throws IOException, InterruptedException
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

		final String prelertTestDataHome = System.getProperty("prelert.test.data.home");
		if (prelertTestDataHome == null)
		{
            throw new IllegalStateException("Error property prelert.test.data.home is not set");
		}

		File flightCentreDataCsv = new File(prelertTestDataHome +
				"/engine_api_integration_test/flightcentre.csv");
		File flightCenterDataCsvGzip = new File(prelertTestDataHome +
				"/engine_api_integration_test/flightcentre.csv.gz");
		File flightCentreDataJson = new File(prelertTestDataHome +
				"/engine_api_integration_test/flightcentre.json");

		try (DataCountsTest test = new DataCountsTest(baseUrl))
		{
			List<String> jobs = new ArrayList<>();

			boolean isJson = false;
			boolean isCompressed = false;
			boolean compareBuckets = true;
			String jobId;

			jobId = test.runFarequoteJob(flightCentreDataCsv, isJson, isCompressed);
			DataCounts counts = test.jobDataCounts(jobId);
			test.validateFlightCentreCounts(counts, isJson, isCompressed, compareBuckets);

			isCompressed = true;
			jobId = test.runFarequoteJob(flightCenterDataCsvGzip, isJson, isCompressed);
			counts = test.jobDataCounts(jobId);
			test.validateFlightCentreCounts(counts, isJson, isCompressed, compareBuckets);

			isJson = true;
			isCompressed = false;
			jobId = test.runFarequoteJob(flightCentreDataJson, isJson, isCompressed);
			counts = test.jobDataCounts(jobId);
			test.validateFlightCentreCounts(counts, isJson, isCompressed, compareBuckets);

			jobs.add(jobId);

			for (String id : jobs)
			{
				test.m_WebServiceClient.deleteJob(id);
			}
		}

		LOGGER.info("All tests passed Ok");
	}
}


