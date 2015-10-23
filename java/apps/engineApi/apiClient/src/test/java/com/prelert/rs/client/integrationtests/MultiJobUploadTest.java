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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.http.client.ClientProtocolException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.Detector;
import com.prelert.job.JobConfiguration;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.rs.client.EngineApiClient;
import com.prelert.rs.data.DataPostResponse;
import com.prelert.rs.data.MultiDataPostResult;
import com.prelert.rs.data.Pagination;

/**
 * Test uploading data to multiple jobs simultaneously using the
 * /data/<jobid1>,<jobid2>,<jobid3>,... URL (i.e. comma separated list
 * of job ids).
 *
 * Each job has the same configuration and is sent (in theory) identical
 * data so the results should also be identical. This test verifies that
 * the multi-upload works and the same results are produced.
 */
public class MultiJobUploadTest
{
    private static final Logger LOGGER = Logger.getLogger(MultiJobUploadTest.class);

    /**
     * The default base Url used in the test
     */
    public static final String API_BASE_URL = "http://localhost:8080/engine/v1";

    private final EngineApiClient m_WebServiceClient;

    public MultiJobUploadTest(String baseUrl)
    {
        m_WebServiceClient = new EngineApiClient(baseUrl);
    }

    private String createFarequoteJob()
    throws ClientProtocolException, IOException
    {
        Detector d = new Detector();
        d.setFieldName("responsetime");
        d.setByFieldName("airline");

        AnalysisConfig ac = new AnalysisConfig();
        ac.setBucketSpan(3600L);
        ac.setDetectors(Arrays.asList(d));

        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.DELIMITED);
        dd.setFieldDelimiter(',');
        dd.setTimeField("time");
        dd.setTimeFormat("yyyy-MM-dd HH:mm:ssX");

        JobConfiguration config = new JobConfiguration(ac);
        config.setDescription("Farequote interim results test");
        config.setDataDescription(dd);

        String jobId = m_WebServiceClient.createJob(config);
        if (jobId == null || jobId.isEmpty())
        {
            LOGGER.error("No Job Id returned by create job");
            test(jobId != null && jobId.isEmpty() == false);
        }

        return jobId;
    }

    private void uploadData(List<String> jobs, File dataFile)
    throws IOException
    {
        FileInputStream stream = new FileInputStream(dataFile);
        MultiDataPostResult results = m_WebServiceClient.streamingUpload(jobs, stream, false);

        test(m_WebServiceClient.getLastError() == null);

        test(results.getResponses().size() == jobs.size());
        test(results.getResponses().size() > 0);

        test(results.getResponses().get(0).getUploadSummary().getInputRecordCount() > 0);
        for (DataPostResponse result : results.getResponses())
        {
            test(jobs.contains(result.getJobId()));
            test(result.getError() == null);
        }
    }

    private void compareBuckets(List<String> jobs)
    throws IOException
    {
        if (jobs.size() <= 1)
        {
            return;
        }

        int skip = 0;
        int take = 100;
        long hitCount = 0;
        do
        {
            Pagination<Bucket> firstPage = m_WebServiceClient
                            .prepareGetBuckets(jobs.get(0)).skip(skip).take(take).get();

            hitCount = firstPage.getHitCount();
            test(hitCount > 0);

            for (int i=1; i<jobs.size(); i++)
            {
                Pagination<Bucket> page = m_WebServiceClient
                            .prepareGetBuckets(jobs.get(i)).skip(skip).take(take).get();

                test(page.getHitCount() == firstPage.getHitCount());
                test(page.getDocumentCount() == firstPage.getDocumentCount());

                test(Objects.equals(firstPage.getDocuments(), page.getDocuments()));
            }

            skip += take;
        }
        while (skip < hitCount);
    }


    private void compareRecords(List<String> jobs)
    throws IOException
    {
        if (jobs.size() <= 1)
        {
            return;
        }

        int skip = 0;
        int take = 100;
        long hitCount = 0;
        do
        {
            Pagination<AnomalyRecord> firstPage = m_WebServiceClient
                                .prepareGetRecords(jobs.get(0)).skip(skip).take(take).get();

            hitCount = firstPage.getHitCount();
            test(hitCount > 0);

            for (int i=1; i<jobs.size(); i++)
            {
                Pagination<AnomalyRecord> page =m_WebServiceClient
                                .prepareGetRecords(jobs.get(0)).skip(skip).take(take).get();

                test(page.getHitCount() == firstPage.getHitCount());
                test(page.getDocumentCount() == firstPage.getDocumentCount());

                test(Objects.equals(firstPage.getDocuments(), page.getDocuments()));
            }

            skip += take;
        }
        while (skip < hitCount);
    }


    private void closeJobs(List<String> jobs)
    throws IOException
    {
        for (String id : jobs)
        {
            m_WebServiceClient.closeJob(id);
        }
    }

    private void deleteJobs(List<String> jobs)
    throws ClientProtocolException, IOException
    {
        for (String id : jobs)
        {
            m_WebServiceClient.deleteJob(id);
        }
    }

    public void doTest(String prelertTestDataHome)
    throws ClientProtocolException, IOException
    {
        List<String> jobIds = new ArrayList<String>();
        jobIds.add(this.createFarequoteJob());
        jobIds.add(this.createFarequoteJob());
        jobIds.add(this.createFarequoteJob());

        File fareQuoteData = new File(prelertTestDataHome +
                "/engine_api_integration_test/farequote.csv");

        LOGGER.info("Uploading data");
        uploadData(jobIds, fareQuoteData);

        closeJobs(jobIds);

        // validate should be identical results
        LOGGER.info("Comparing bucket results");
        compareBuckets(jobIds);

        LOGGER.info("Comparing anomaly records");
        compareRecords(jobIds);

        LOGGER.info("Deleting jobs");
        deleteJobs(jobIds);
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

        MultiJobUploadTest test = new MultiJobUploadTest(baseUrl);
        test.doTest(prelertTestDataHome);

        LOGGER.info("Multi-Upload test completed successfully");
    }
}
