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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.Detector;
import com.prelert.job.JobConfiguration;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.rs.client.EngineApiClient;
import com.prelert.rs.data.ApiError;
import com.prelert.rs.data.MultiDataPostResult;
import com.prelert.rs.data.Pagination;


/**
 * Integration test for the bucket resetting functionality.
 * <br>
 * The system property 'prelert.test.data.home' must be set and point
 * to a directory containing the file:
 * <ol>
 * <li>/engine_api_integration_test/bucket_resetting/farequote-start.csv</li>
 * <li>/engine_api_integration_test/bucket_resetting/jbu-high-count.txt</li>
 * <li>/engine_api_integration_test/bucket_resetting/non-jbu-hour.txt</li>
 * </ol>
 *
 */
public class BucketResettingTest implements Closeable
{
    private static final Logger LOGGER = Logger.getLogger(BucketResettingTest.class);

    private static final long BUCKET_SPAN = 3600;
    private static final int LATENCY_BUCKETS = 2;
    static final String TEST_JOB_ID = "farequote-bucket-resetting-test";

    /**
     * The default base Url used in the test
     */
    public static final String API_BASE_URL = "http://localhost:8080/engine/v2";

    private final EngineApiClient m_WebServiceClient;
    private final String m_TestDataHome;
    private final String m_BaseUrl;

    /**
     * Creates a new http client call {@linkplain #close()} once finished
     */
    public BucketResettingTest(String prelertTestDataHome, String baseUrl)
    {
        m_WebServiceClient = new EngineApiClient(baseUrl);
        m_TestDataHome = prelertTestDataHome;
        m_BaseUrl = baseUrl;
    }

    @Override
    public void close() throws IOException
    {
        m_WebServiceClient.close();
    }

    public String createFarequoteJob() throws IOException
    {
        Detector d = new Detector();
        d.setFunction("non_zero_count");
        d.setByFieldName("airline");

        AnalysisConfig ac = new AnalysisConfig();
        ac.setBucketSpan(BUCKET_SPAN);
        ac.setOverlappingBuckets(false);
        ac.setLatency(LATENCY_BUCKETS * BUCKET_SPAN);
        ac.setDetectors(Arrays.asList(d));

        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.DELIMITED);
        dd.setFieldDelimiter(',');
        dd.setTimeField("time");
        dd.setTimeFormat("yyyy-MM-dd HH:mm:ssX");

        JobConfiguration config = new JobConfiguration(ac);
        config.setDescription("Farequote bucket resetting test");
        config.setId(TEST_JOB_ID);
        config.setDataDescription(dd);

        String jobId = m_WebServiceClient.createJob(config);
        if (jobId == null || jobId.isEmpty())
        {
            LOGGER.error("No Job Id returned by create job");
            test(jobId != null && jobId.isEmpty() == false);
        }
        test(TEST_JOB_ID.equals(jobId));

        return jobId;
    }


    public void verifyBucketResetting() throws IOException, InterruptedException
    {
        // Introduce anomaly in most recent bucket @ 2014-06-27 12:00:00Z
        File anomalousData = new File(m_TestDataHome
                + "/engine_api_integration_test/bucket_resetting/jbu-high-count.txt");
        m_WebServiceClient.fileUpload(TEST_JOB_ID, anomalousData, false);
        m_WebServiceClient.flushJob(TEST_JOB_ID, true);

        String start = "2014-06-27T12:00:00Z";
        String end = "2014-06-27T13:00:00Z";

        Pagination<Bucket> buckets = m_WebServiceClient.prepareGetBuckets(TEST_JOB_ID)
                .includeInterim(true).start(start).end(end).get();
        test(buckets.getHitCount() == 1);
        Bucket bucket = buckets.getDocuments().get(0);
        test(bucket.getRecordCount() == 1);

        List<AnomalyRecord> records = getInterimResultsOnly(start, end);
        test(records.size() == 1);

        // Reset bucket and fill with normal data
        File normalData = new File(m_TestDataHome
                + "/engine_api_integration_test/bucket_resetting/non-jbu-hour.txt");
        m_WebServiceClient.fileUpload(TEST_JOB_ID, normalData, false, start, end);
        m_WebServiceClient.flushJob(TEST_JOB_ID, true);

        buckets = m_WebServiceClient.prepareGetBuckets(TEST_JOB_ID)
                .includeInterim(true).start(start).end(end).get();
        test(buckets.getHitCount() == 1);
        bucket = buckets.getDocuments().get(0);
        test(bucket.getMaxNormalizedProbability() < 1.0);

        records = getInterimResultsOnly(start, end);
        for (AnomalyRecord record : records)
        {
            test(record.getNormalizedProbability() < 1.0);
        }

        // Finally delete first two available buckets and leave them empty
        start = "2014-06-27T10:00:00Z";
        end = "2014-06-27T12:00:00Z";
        buckets = m_WebServiceClient.prepareGetBuckets(TEST_JOB_ID)
                .includeInterim(true).start(start).end(end).get();
        test(buckets.getHitCount() == 2);
        test(buckets.getDocuments().get(0).getEventCount() > 0);
        test(buckets.getDocuments().get(1).getEventCount() > 0);

        String input = "time,airline,responsetime,sourcetype\n";
        input += "2014-06-27 12:00:00Z,AAL,42.0,farequote\n";
        BufferedInputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(
                input.getBytes(StandardCharsets.UTF_8)));
        MultiDataPostResult result = m_WebServiceClient.streamingUpload(TEST_JOB_ID,
                inputStream, false, start, end);

        test(result.anErrorOccurred() == false);
        test(result.getResponses().size() == 1);
        ApiError error = result.getResponses().get(0).getError();
        test(error == null);
        test(result.getResponses().get(0).getUploadSummary().getProcessedRecordCount() == 1);

        m_WebServiceClient.flushJob(TEST_JOB_ID, true);

        buckets = m_WebServiceClient.prepareGetBuckets(TEST_JOB_ID)
                .includeInterim(true).start(start).end(end).get();
        test(buckets.getHitCount() == 2);
        test(buckets.getDocuments().get(0).getEventCount() == 0);
        test(buckets.getDocuments().get(1).getEventCount() == 0);
    }

    private List<AnomalyRecord> getInterimResultsOnly(String start, String end) throws IOException
    {
        Pagination<AnomalyRecord> page = m_WebServiceClient.prepareGetRecords(TEST_JOB_ID)
                .start(start).end(end).includeInterim(true).get();
        List<AnomalyRecord> records = page.getDocuments();
        for (AnomalyRecord record : records)
        {
            test(record.isInterim());
        }
        return records;
    }

    /**
     * Delete all the jobs in the list of job ids
     *
     * @param jobIds The list of ids of the jobs to delete
     * @throws IOException
     * @throws InterruptedException
     */
    public void deleteJobs(List<String> jobIds)
    throws IOException, InterruptedException
    {
        for (String jobId : jobIds)
        {
            LOGGER.debug("Deleting job " + jobId);

            boolean success = m_WebServiceClient.deleteJob(jobId);
            if (success == false)
            {
                LOGGER.error("Error deleting job " + m_BaseUrl + "/" + jobId);
            }
        }
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


    /**
     * The program takes one argument which is the base Url of the RESTful API.
     * If no arguments are given then {@value #API_BASE_URL} is used.
     *
     * @param args
     * @throws IOException
     * @throws InterruptedException
     */
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

        String farequoteJob = TEST_JOB_ID;

        List<String> jobUrls = new ArrayList<>();
        try (BucketResettingTest test = new BucketResettingTest(prelertTestDataHome, baseUrl))
        {
            // Always delete the test job first in case it is hanging around
            // from a previous run
            test.m_WebServiceClient.deleteJob(farequoteJob);
            jobUrls.add(farequoteJob);

            File fareQuotePartData = new File(prelertTestDataHome +
                    "/engine_api_integration_test/bucket_resetting/farequote-start.csv");

            test.createFarequoteJob();
            test.m_WebServiceClient.fileUpload(farequoteJob, fareQuotePartData, false);

            test.verifyBucketResetting();

            //==========================
            // Clean up test jobs
            BucketResettingTest.test(test.m_WebServiceClient.closeJob(farequoteJob) == true);
            test.deleteJobs(jobUrls);
        }

        LOGGER.info("All tests passed Ok");
    }
}
