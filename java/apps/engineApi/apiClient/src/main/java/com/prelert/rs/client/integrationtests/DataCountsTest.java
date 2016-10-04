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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataCounts;
import com.prelert.job.DataDescription;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.Detector;
import com.prelert.job.JobConfiguration;
import com.prelert.job.JobDetails;
import com.prelert.rs.data.ApiError;
import com.prelert.rs.data.MultiDataPostResult;
import com.prelert.rs.data.SingleDocument;

/**
 * Test the data counts (num records, fields, buckets, etc)
 * after uploading data to a job. Verifys both the counts
 * returned by the data upload and the counts object inside
 * the {@linkplain JobDetails} instance.
 */
public class DataCountsTest extends BaseIntegrationTest
{
	static final long FLIGHTCENTRE_NUM_BUCKETS = 296;
	static final long FLIGHTCENTRE_NUM_RECORDS = 175910;
	static final long FLIGHTCENTRE_NUM_FIELDS = FLIGHTCENTRE_NUM_RECORDS * 3;
	static final long FLIGHTCENTRE_NUM_PROCESSED_FIELDS = FLIGHTCENTRE_NUM_RECORDS * 2;
	static final long FLIGHTCENTRE_INPUT_BYTES_CSV = 5724086;

	static final long FLIGHTCENTRE_INPUT_BYTES_JSON = 17510020;

	public DataCountsTest(String baseUrl)
	{
	    super(baseUrl, true);
	}

	@Override
	protected void runTest() throws IOException
	{

        File flightCentreDataCsv = new File(m_TestDataHome +
                "/engine_api_integration_test/flightcentre.csv");
        File flightCenterDataCsvGzip = new File(m_TestDataHome +
                "/engine_api_integration_test/flightcentre.csv.gz");
        File flightCentreDataJson = new File(m_TestDataHome +
                "/engine_api_integration_test/flightcentre.json");

        List<String> jobs = new ArrayList<>();

        boolean isJson = false;
        boolean isCompressed = false;
        boolean compareBuckets = true;
        String jobId;

        jobId = runFarequoteJob(flightCentreDataCsv, isJson, isCompressed);
        DataCounts counts = jobDataCounts(jobId);
        validateFlightCentreCounts(counts, isJson, isCompressed, compareBuckets);

        isCompressed = true;
        jobId = runFarequoteJob(flightCenterDataCsvGzip, isJson, isCompressed);
        counts = jobDataCounts(jobId);
        validateFlightCentreCounts(counts, isJson, isCompressed, compareBuckets);

        isJson = true;
        isCompressed = false;
        jobId = runFarequoteJob(flightCentreDataJson, isJson, isCompressed);
        counts = jobDataCounts(jobId);
        validateFlightCentreCounts(counts, isJson, isCompressed, compareBuckets);

        jobs.add(jobId);

        deleteJobs(jobs);
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

		String jobId = m_EngineApiClient.createJob(config);
		if (jobId == null || jobId.isEmpty())
		{
			m_Logger.error("No Job Id returned by create job");
			m_Logger.info(m_EngineApiClient.getLastError().toJson());
			test(jobId != null && jobId.isEmpty() == false);
		}

		MultiDataPostResult result = m_EngineApiClient.fileUpload(jobId, dataFile, compressed);

        test(result.anErrorOccurred() == false);
        test(result.getResponses().size() == 1);
        ApiError error = result.getResponses().get(0).getError();
        test(error == null);

		if (result.getResponses().get(0).getUploadSummary().getInputRecordCount() == 0)
		{
			m_Logger.error(error.toJson());
			test(false);
		}

		validateFlightCentreCounts(result.getResponses().get(0).getUploadSummary(),
		                        isJson, compressed, false);

		m_EngineApiClient.closeJob(jobId);


		return jobId;
	}

	private DataCounts jobDataCounts(String jobId) throws IOException
	{
	    SingleDocument<JobDetails> job =  m_EngineApiClient.getJob(jobId);
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

	public static void main(String[] args)
	throws IOException, InterruptedException
	{
		String baseUrl = API_BASE_URL;
		if (args.length > 0)
		{
			baseUrl = args[0];
		}

		try (DataCountsTest test = new DataCountsTest(baseUrl))
		{
		    test.runTest();

			test.m_Logger.info("All tests passed Ok");
		}
	}
}


