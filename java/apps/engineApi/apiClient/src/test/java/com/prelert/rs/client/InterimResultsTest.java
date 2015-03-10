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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
import com.prelert.rs.data.AnomalyRecord;
import com.prelert.rs.data.Bucket;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;


/**
 * Tests the interim results for both buckets and records endpoints.
 * <br/>
 * The system property 'prelert.test.data.home' must be set and point
 * to a directory containing the file:
 * <ol>
 * <li>/engine_api_integration_test/farequote_part.csv</li>
 * </ol>
 *
 */
public class InterimResultsTest implements Closeable
{
    private static final Logger LOGGER = Logger.getLogger(JobsTest.class);

    static final long FAREQUOTE_NUM_FINAL_BUCKETS = 770;

    static final String TEST_JOB_ID = "farequote-interim-test";

    /**
     * The default base Url used in the test
     */
    public static final String API_BASE_URL = "http://localhost:8080/engine/v1";

    private EngineApiClient m_WebServiceClient;


    /**
     * Creates a new http client call {@linkplain #close()} once finished
     */
    public InterimResultsTest()
    {
        m_WebServiceClient = new EngineApiClient();
    }

    @Override
    public void close() throws IOException
    {
        m_WebServiceClient.close();
    }


    public String createFarequoteJob(String apiUrl)
    throws ClientProtocolException, IOException
    {
        Detector d = new Detector();
        d.setFieldName("responsetime");
        d.setByFieldName("airline");

        AnalysisConfig ac = new AnalysisConfig();
        ac.setBucketSpan(300L);
        ac.setDetectors(Arrays.asList(d));

        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.DELINEATED);
        dd.setFieldDelimiter(',');
        dd.setTimeField("time");
        dd.setTimeFormat("yyyy-MM-dd HH:mm:ssX");

        JobConfiguration config = new JobConfiguration(ac);
        config.setDescription("Farequote interim results test");
        config.setId(TEST_JOB_ID);
        config.setDataDescription(dd);

        String jobId = m_WebServiceClient.createJob(apiUrl, config);
        if (jobId == null || jobId.isEmpty())
        {
            LOGGER.error("No Job Id returned by create job");
            test(jobId != null && jobId.isEmpty() == false);
        }
        test(jobId.equals(TEST_JOB_ID));

        return jobId;
    }


    /**
     * Verify results that are considered final.
     * <ol>
     * <li>There should not be any results for 16:10 on 30th January 2013</li>
     * </ol>
     *
     * @param baseUrl
     * @param jobId
     * @param includeInterim
     * @return
     * @throws IOException
     */
    public boolean verifyFarequoteInterimBuckets(String baseUrl, String jobId,
            boolean includeInterim)
    throws IOException
    {
        long expectedBuckets = (includeInterim ? FAREQUOTE_NUM_FINAL_BUCKETS + 1 : FAREQUOTE_NUM_FINAL_BUCKETS);

        Pagination<Bucket> allBuckets = m_WebServiceClient.getBuckets(baseUrl,
                jobId, false, includeInterim, 0l, 1500l, 0.0, 0.0);
        test(allBuckets.getDocumentCount() == expectedBuckets);
        test(allBuckets.getHitCount() == expectedBuckets);

        /*
         * Test that getting all the results at once is the same as
         * paging them.
         */
        List<Bucket> pagedBuckets = new ArrayList<>();
        long skip = 0, take = 100;
        while (skip < expectedBuckets)
        {
            Pagination<Bucket> buckets = m_WebServiceClient.getBuckets(baseUrl,
                    jobId, false, includeInterim, skip, take, 0.0, 0.0);
            pagedBuckets.addAll(buckets.getDocuments());

            skip += take;
        }
        test(pagedBuckets.size() == expectedBuckets);

        for (int i = 0; i < expectedBuckets; i++)
        {
            test(Double.compare(pagedBuckets.get(i).getAnomalyScore(),
                                allBuckets.getDocuments().get(i).getAnomalyScore()) == 0);

            test(Double.compare(pagedBuckets.get(i).getMaxNormalizedProbability(),
                    allBuckets.getDocuments().get(i).getMaxNormalizedProbability()) == 0);

            test(pagedBuckets.get(i).equals(allBuckets.getDocuments().get(i)));

            if (i < FAREQUOTE_NUM_FINAL_BUCKETS)
            {
                test(pagedBuckets.get(i).isInterim() == null || pagedBuckets.get(i).isInterim() == false);
            }
            else
            {
                test(pagedBuckets.get(i).isInterim() != null && pagedBuckets.get(i).isInterim() == true);
            }
        }

        /*
         * Test that paging buckets by date is the same
         */
        pagedBuckets = new ArrayList<>();
        skip = 0; take = 140;
        while (skip < allBuckets.getHitCount())
        {
            Pagination<Bucket> buckets = m_WebServiceClient.getBuckets(baseUrl,
                    jobId, false, includeInterim, skip, take,
                    allBuckets.getDocuments().get(0).getEpoch(),
                    allBuckets.getDocuments().get((int)allBuckets.getHitCount() - 1).getEpoch() + 1,
                    0.0, 0.0);

            pagedBuckets.addAll(buckets.getDocuments());

            skip += take;
        }
        test(pagedBuckets.size() == allBuckets.getDocumentCount());

        for (int i = 0; i < pagedBuckets.size(); i++)
        {
            test(Double.compare(pagedBuckets.get(i).getAnomalyScore(),
                                allBuckets.getDocuments().get(i).getAnomalyScore()) == 0);

            test(Double.compare(pagedBuckets.get(i).getMaxNormalizedProbability(),
                    allBuckets.getDocuments().get(i).getMaxNormalizedProbability()) == 0);

            test(pagedBuckets.get(i).equals(allBuckets.getDocuments().get(i)));

            if (i < FAREQUOTE_NUM_FINAL_BUCKETS)
            {
                test(pagedBuckets.get(i).isInterim() == null || pagedBuckets.get(i).isInterim() == false);
            }
            else
            {
                test(pagedBuckets.get(i).isInterim() != null && pagedBuckets.get(i).isInterim() == true);
            }
        }

        /*
         * Test get buckets by date range with a time string
         */
        String [] startDateFormats = new String[] {"2013-01-29T05:10:00Z", "1359436200"};
        String [] endDateFormats = new String[] {"2013-01-30T16:15:00.000+0000", "1359562500"};

        for (int i = 0; i < startDateFormats.length; i++)
        {
            Pagination<Bucket> byDate = m_WebServiceClient.getBuckets(
                    baseUrl, jobId, false, includeInterim, 0l, 1000l,
                    startDateFormats[i], endDateFormats[i],
                    0.0, 0.0);

            test(byDate.getDocuments().get(0).getEpoch() == 1359436200l);
            if (includeInterim)
            {
                test(byDate.getDocuments().get(byDate.getDocumentCount() - 1).getEpoch() == 1359562200l);
            }
            else
            {
                test(byDate.getDocuments().get(byDate.getDocumentCount() - 1).getEpoch() == 1359561900l);
            }

            int startIndex = Collections.binarySearch(allBuckets.getDocuments(),
                    byDate.getDocuments().get(0),
                    new Comparator<Bucket>() {
                @Override
                public int compare(Bucket o1, Bucket o2)
                {
                    return Long.compare(o1.getEpoch(), o2.getEpoch());
                }
            });

            test(startIndex >= 0);
            for (int j = 0; j < byDate.getDocumentCount(); j++)
            {
                test(Double.compare(byDate.getDocuments().get(j).getAnomalyScore(),
                        allBuckets.getDocuments().get(j + startIndex).getAnomalyScore()) == 0);

                test(Double.compare(pagedBuckets.get(i).getMaxNormalizedProbability(),
                        allBuckets.getDocuments().get(i).getMaxNormalizedProbability()) == 0);

                test(byDate.getDocuments().get(j).equals(allBuckets.getDocuments().get(j + startIndex)));
            }
        }

        // The big anomaly is in bucket 771, which will only be present if
        // interim results have been requested.
        if (includeInterim)
        {
            test(Boolean.TRUE.equals(pagedBuckets.get(770).isInterim()));
            test(pagedBuckets.get(770).getAnomalyScore() >= 90.0);
        }

        Pagination<Bucket> allBucketsExpanded = m_WebServiceClient.getBuckets(baseUrl,
                jobId, true, includeInterim, 0l, 1500l, 0.0, 0.0);

        /*
         * The bucket unusual score is the max unusual score
         * and the anomaly score is the same as for each of the
         * records
         */
        for (Bucket bucket: allBucketsExpanded.getDocuments())
        {
            double bucketMax = 0.0;
            for (AnomalyRecord r : bucket.getRecords())
            {
                bucketMax = Math.max(r.getNormalizedProbability(), bucketMax);
                test(r.isInterim() == bucket.isInterim());
            }

            test(bucketMax == bucket.getMaxNormalizedProbability());
        }

        /*
         * Check the bucket unusual score is equal to the max unusual
         * record score in the bucket and every record in the bucket has
         * the same anomaly score as the bucket.
         */
        // the test takes ages to go through every bucket, don't do all for now
        int toIgnore = 750;
        for (Bucket bucket: allBucketsExpanded.getDocuments())
        {
            if (--toIgnore > 0)
            {
                continue;
            }

            SingleDocument<Bucket> bucketDoc = m_WebServiceClient.getBucket(
                    baseUrl, jobId, bucket.getId(), true, includeInterim);

            List<AnomalyRecord> records = bucketDoc.getDocument().getRecords();

            double bucketMax = 0.0;
            for (AnomalyRecord r : records)
            {
                bucketMax = Math.max(r.getNormalizedProbability(), bucketMax);

                test(r.getAnomalyScore() == bucket.getAnomalyScore());
                test(r.isInterim() == bucket.isInterim());
            }

            test(bucketMax == bucket.getMaxNormalizedProbability());
        }

        return true;
    }


    /**
     * Get records via the 'records' end point and check that interim
     * results are included if and only if requested.
     *
     * @param baseUrl
     * @param jobId
     * @param includeInterim
     * @return
     * @throws IOException
     */
    public boolean verifyFarequoteInterimRecords(String baseUrl, String jobId,
            boolean includeInterim)
    throws IOException
    {
        Pagination<AnomalyRecord> allRecords = m_WebServiceClient.getRecords(
                baseUrl, jobId, 0l, 3000l, includeInterim);

        /*
         * Test that getting all the results at once is the same as
         * paging them.
         */
        List<AnomalyRecord> pagedRecords = new ArrayList<>();
        long skip = 0, take = 1000;

        Pagination<AnomalyRecord> page = m_WebServiceClient.getRecords(
                baseUrl, jobId, skip, take, includeInterim);
        skip += take;
        pagedRecords.addAll(page.getDocuments());

        while (skip < page.getHitCount())
        {
            page = m_WebServiceClient.getRecords(baseUrl, jobId, skip, take, includeInterim);
            skip += take;
            pagedRecords.addAll(page.getDocuments());
        }

        int recordIndex = 0;
        for (AnomalyRecord record : allRecords.getDocuments())
        {
            test(record.equals(pagedRecords.get(recordIndex)));
            recordIndex++;
        }
        test(recordIndex == pagedRecords.size());

        /*
         * Test paging by date is the same as getting them all
         * at once
         */

        // need start and end dates first
        Pagination<Bucket> allBuckets = m_WebServiceClient.getBuckets(baseUrl,
                jobId, true, includeInterim, 0l, 3000l, 0.0, 0.0);
        long startDate = allBuckets.getDocuments().get(0).getEpoch();
        long endDate = allBuckets.getDocuments().get(allBuckets.getDocumentCount() - 1).getEpoch() + 1;

        pagedRecords = new ArrayList<>();
        skip = 0; take = 200;

        page = m_WebServiceClient.getRecords(
                baseUrl, jobId, skip, take,
                startDate, endDate, includeInterim);
        skip += take;
        pagedRecords.addAll(page.getDocuments());

        while (skip < page.getHitCount())
        {
            page = m_WebServiceClient.getRecords(
                    baseUrl, jobId, skip, take,
                    startDate, endDate, includeInterim);

            skip += take;
            pagedRecords.addAll(page.getDocuments());
        }

        recordIndex = 0;
        test(allRecords.getHitCount() == pagedRecords.size());

        for (AnomalyRecord record : allRecords.getDocuments())
        {
            test(record.equals(pagedRecords.get(recordIndex)));
            recordIndex++;
        }
        test(recordIndex == pagedRecords.size());


        /*
         * There should be one anomaly with unusual score of 90+
         * and at least one with anomaly score 90+
         */
        int numInterimRecords = 0;
        for (AnomalyRecord record : pagedRecords)
        {
            if (Boolean.TRUE.equals(record.isInterim()))
            {
                ++numInterimRecords;
            }
        }

        if (includeInterim)
        {
            test(numInterimRecords == 3);
        }
        else
        {
            test(numInterimRecords == 0);
        }

        return true;
    }


    /**
     * Delete all the jobs in the list of job ids
     *
     * @param baseUrl The URL of the REST API i.e. an URL like
     *     <code>http://prelert-host:8080/engine/version/</code>
     * @param jobIds The list of ids of the jobs to delete
     * @throws IOException
     * @throws InterruptedException
     */
    public void deleteJobs(String baseUrl, List<String> jobIds)
    throws IOException, InterruptedException
    {
        for (String jobId : jobIds)
        {
            LOGGER.debug("Deleting job " + jobId);

            boolean success = m_WebServiceClient.deleteJob(baseUrl, jobId);
            if (success == false)
            {
                LOGGER.error("Error deleting job " + baseUrl + "/" + jobId);
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
            LOGGER.error("Error property prelert.test.data.home is not set");
            return;
        }

        String farequoteJob = TEST_JOB_ID;

        InterimResultsTest test = new InterimResultsTest();
        List<String> jobUrls = new ArrayList<>();

        // Always delete the test job first in case it is hanging around
        // from a previous run
        test.m_WebServiceClient.deleteJob(baseUrl, farequoteJob);
        jobUrls.add(farequoteJob);

        File fareQuotePartData = new File(prelertTestDataHome +
                "/engine_api_integration_test/farequote_part.csv");
        // Farequote test
        test.createFarequoteJob(baseUrl);
        test.m_WebServiceClient.fileUpload(baseUrl, farequoteJob,
                fareQuotePartData, false);
        InterimResultsTest.test(test.m_WebServiceClient.flushJob(baseUrl, farequoteJob, true) == true);
        InterimResultsTest.test(test.m_WebServiceClient.closeJob(baseUrl, farequoteJob) == true);

        test.verifyFarequoteInterimBuckets(baseUrl, farequoteJob, false);
        test.verifyFarequoteInterimBuckets(baseUrl, farequoteJob, true);
        test.verifyFarequoteInterimRecords(baseUrl, farequoteJob, false);
        test.verifyFarequoteInterimRecords(baseUrl, farequoteJob, true);

        //==========================
        // Clean up test jobs
        test.deleteJobs(baseUrl, jobUrls);

        test.close();

        LOGGER.info("All tests passed Ok");
    }
}
