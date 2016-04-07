/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
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
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.Detector;
import com.prelert.job.JobConfiguration;
import com.prelert.job.alert.Alert;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.job.results.Influencer;
import com.prelert.rs.client.AlertRequestBuilder;
import com.prelert.rs.client.EngineApiClient;
import com.prelert.rs.client.integrationtests.alertpoll.PollAlertService;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;


/**
 * Tests the interim results for both buckets and records endpoints.
 * Also poll for an alert from an interim bucket result
 * <br>
 * The system property 'prelert.test.data.home' must be set and point
 * to a directory containing the file:
 * <ol>
 * <li>/engine_api_integration_test/farequote_part.csv</li>
 * </ol>
 *
 */
public class InterimResultsTest implements Closeable
{
    private static final Logger LOGGER = Logger.getLogger(InterimResultsTest.class);

    private static final long BUCKET_SPAN = 300;

    private static final int LATENCY_BUCKETS = 2;

    // Total number of buckets in data (this includes the current bucket at the end of the job)
    static final long FAREQUOTE_NUM_TOTAL_BUCKETS = 771;

    // That is the total number of buckets
    static final long FAREQUOTE_NUM_BEFORE_INTERIM_RESULTS_BUCKETS =
            FAREQUOTE_NUM_TOTAL_BUCKETS - LATENCY_BUCKETS - 1;

    static final String TEST_JOB_ID = "farequote-interim-test";

    /**
     * The default base Url used in the test
     */
    public static final String API_BASE_URL = "http://localhost:8080/engine/v2";

    private final EngineApiClient m_WebServiceClient;
    private final PollAlertService m_PollAlertService;
    private final String m_BaseUrl;


    /**
     * Creates a new http client call {@linkplain #close()} once finished
     */
    public InterimResultsTest(String baseUrl)
    {
        m_WebServiceClient = new EngineApiClient(baseUrl);
        m_BaseUrl = baseUrl;
        m_PollAlertService = new PollAlertService();
    }

    @Override
    public void close() throws IOException
    {
        m_WebServiceClient.close();
    }


    public String createFarequoteJob() throws IOException
    {
        Detector d = new Detector();
        d.setFieldName("responsetime");
        d.setByFieldName("airline");

        AnalysisConfig ac = new AnalysisConfig();
        ac.setBucketSpan(BUCKET_SPAN);
        ac.setLatency(LATENCY_BUCKETS * BUCKET_SPAN);
        ac.setDetectors(Arrays.asList(d));
        ac.setInfluencers(Arrays.asList("responsetime", "airline"));

        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.DELIMITED);
        dd.setFieldDelimiter(',');
        dd.setTimeField("time");
        dd.setTimeFormat("yyyy-MM-dd HH:mm:ssX");

        JobConfiguration config = new JobConfiguration(ac);
        config.setDescription("Farequote interim results test");
        config.setId(TEST_JOB_ID);
        config.setDataDescription(dd);

        String jobId = m_WebServiceClient.createJob(config);
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
     * @param jobId
     * @param includeInterim
     * @return
     * @throws IOException
     */
    public boolean verifyFarequoteInterimBuckets(String jobId, boolean includeInterim)
    throws IOException
    {
        long expectedBuckets = (includeInterim ? FAREQUOTE_NUM_TOTAL_BUCKETS :
                FAREQUOTE_NUM_BEFORE_INTERIM_RESULTS_BUCKETS);

        Pagination<Bucket> allBuckets = m_WebServiceClient.prepareGetBuckets(jobId)
                .includeInterim(includeInterim).take(1500).get();
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
            Pagination<Bucket> buckets = m_WebServiceClient.prepareGetBuckets(jobId)
                    .includeInterim(includeInterim).skip(skip).take(take).get();
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

            if (i < FAREQUOTE_NUM_BEFORE_INTERIM_RESULTS_BUCKETS)
            {
                test(pagedBuckets.get(i).isInterim() == false);
            }
            else
            {
                test(pagedBuckets.get(i).isInterim() == true);
            }
        }

        /*
         * Test that paging buckets by date is the same
         */
        pagedBuckets = new ArrayList<>();
        skip = 0; take = 140;
        while (skip < allBuckets.getHitCount())
        {
            Pagination<Bucket> buckets = m_WebServiceClient.prepareGetBuckets(jobId)
                    .includeInterim(includeInterim)
                    .skip(skip)
                    .take(take)
                    .start(allBuckets.getDocuments().get(0).getEpoch())
                    .end(allBuckets.getDocuments().get((int)allBuckets.getHitCount() - 1).getEpoch() + 1)
                    .get();

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

            if (i < FAREQUOTE_NUM_BEFORE_INTERIM_RESULTS_BUCKETS)
            {
                test(pagedBuckets.get(i).isInterim() == false);
            }
            else
            {
                test(pagedBuckets.get(i).isInterim() == true);
            }
        }

        /*
         * Test get buckets by date range with a time string
         */
        String [] startDateFormats = new String[] {"2013-01-29T05:10:00Z", "1359436200"};
        String [] endDateFormats = new String[] {"2013-01-30T16:15:00.000+00:00", "1359562500"};

        for (int i = 0; i < startDateFormats.length; i++)
        {
            Pagination<Bucket> byDate = m_WebServiceClient.prepareGetBuckets(jobId)
                    .includeInterim(includeInterim)
                    .take(1000)
                    .start(startDateFormats[i])
                    .end(endDateFormats[i])
                    .get();

            test(byDate.getDocuments().get(0).getEpoch() == 1359436200l);
            if (includeInterim)
            {
                test(byDate.getDocuments().get(byDate.getDocumentCount() - 1).getEpoch() == 1359562200l);
            }
            else
            {
                test(byDate.getDocuments().get(byDate.getDocumentCount() - 1).getEpoch() == 1359561300l);
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

        // The big anomaly is in bucket 771, which will only be present
        // if interim results have been requested.
        if (includeInterim)
        {
            test(Boolean.TRUE.equals(pagedBuckets.get(770).isInterim()));
            test(pagedBuckets.get(770).getAnomalyScore() >= 60.0);
        }

        Pagination<Bucket> allBucketsExpanded = m_WebServiceClient.prepareGetBuckets(jobId)
                .expand(true)
                .includeInterim(includeInterim)
                .take(1500).get();

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

            SingleDocument<Bucket> bucketDoc =
                    m_WebServiceClient.prepareGetBucket(jobId, Long.toString(bucket.getEpoch()))
                            .expand(true)
                            .includeInterim(includeInterim).get();

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
     * @param jobId
     * @param includeInterim
     * @return
     * @throws IOException
     */
    public boolean verifyFarequoteInterimRecords(String jobId, boolean includeInterim)
    throws IOException
    {
        Pagination<AnomalyRecord> allRecords = m_WebServiceClient.prepareGetRecords(jobId)
                .take(3000).includeInterim(includeInterim).get();

        /*
         * Test that getting all the results at once is the same as
         * paging them.
         */
        List<AnomalyRecord> pagedRecords = new ArrayList<>();
        long skip = 0, take = 1000;

        Pagination<AnomalyRecord> page = m_WebServiceClient.prepareGetRecords(jobId)
                .skip(skip).take(take).includeInterim(includeInterim).get();
        skip += take;
        pagedRecords.addAll(page.getDocuments());

        while (skip < page.getHitCount())
        {
            page = m_WebServiceClient.prepareGetRecords(jobId).skip(skip).take(take)
                    .includeInterim(includeInterim).get();
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
        Pagination<Bucket> allBuckets = m_WebServiceClient.prepareGetBuckets(jobId)
                .expand(true)
                .includeInterim(includeInterim)
                .take(3000).get();
        long startDate = allBuckets.getDocuments().get(0).getEpoch();
        long endDate = allBuckets.getDocuments().get(allBuckets.getDocumentCount() - 1).getEpoch() + 1;

        pagedRecords = new ArrayList<>();
        skip = 0; take = 200;

        page = m_WebServiceClient.prepareGetRecords(jobId).skip(skip).take(take).start(startDate)
                .end(endDate).includeInterim(includeInterim).get();
        skip += take;
        pagedRecords.addAll(page.getDocuments());

        while (skip < page.getHitCount())
        {
            page = m_WebServiceClient.prepareGetRecords(jobId).skip(skip).take(take)
                    .start(startDate).end(endDate).includeInterim(includeInterim).get();

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

    public void verifyInterimResultsAreRecalculated() throws IOException
    {
        String start = "2013-01-30T16:00:00Z";
        String end = "2013-01-30T16:10:00Z";
        List<AnomalyRecord> interimResults = getInterimResultsOnly(start, end);
        test(interimResults.size() == 0);

        String input = "time,airline,responsetime,sourcetype\n";
        input += "2013-01-30 16:08:00Z,AAL,50000.0,farequote\n";
        BufferedInputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(
                input.getBytes(StandardCharsets.UTF_8)));
        m_WebServiceClient.streamingUpload(TEST_JOB_ID, inputStream, false);
        m_WebServiceClient.flushJob(TEST_JOB_ID, true, start, end);

        interimResults = getInterimResultsOnly(start, end);
        test(interimResults.size() == 1);
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX");
        AnomalyRecord record = interimResults.get(0);
        ZonedDateTime timestamp = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(record.getTimestamp().getTime()), ZoneOffset.UTC);
        test(dateFormat.format(timestamp).equals("2013-01-30T16:05:00Z"));
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
     * Check we get interim influencer results.
     *
     * @param jobId
     * @param includeInterim
     * @throws IOException
     */
    public void verifyFarequoteInterimInfluencers(String jobId, boolean includeInterim)
    throws IOException
    {
        Pagination<Influencer> allInfluencers = m_WebServiceClient.prepareGetInfluencers(jobId)
                .take(10000).includeInterim(includeInterim)
                .sortField("@timestamp")
                .get();


        int numInterim = 0;
        for (Influencer inf : allInfluencers.getDocuments())
        {
            if (inf.isInterim())
            {
                ++numInterim;
            }
        }

        if (includeInterim)
        {
            test(numInterim > 0);
        }
        else
        {
            test(numInterim == 0);
        }
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
     * @throws ExecutionException
     */
    public static void main(String[] args)
    throws IOException, InterruptedException, ExecutionException
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


        List<String> jobUrls = new ArrayList<>();
        try (InterimResultsTest test = new InterimResultsTest(baseUrl))
        {
            // Always delete the test job first in case it is hanging around
            // from a previous run
            test.m_WebServiceClient.deleteJob(TEST_JOB_ID);
            jobUrls.add(TEST_JOB_ID);

            File fareQuotePartData = new File(prelertTestDataHome +
                    "/engine_api_integration_test/farequote_part.csv");

            // Farequote test
            test.createFarequoteJob();

            test.m_WebServiceClient.fileUpload(TEST_JOB_ID, fareQuotePartData, false);

            // Wait a few seconds for the Engine to finish processing the uploaded data
            // We want to register for an alert in response to the flush
            try
            {
                Thread.sleep(5000);
            }
            catch (InterruptedException e)
            {

            }

            // Register for alerts
            AlertRequestBuilder requestBuilder = new AlertRequestBuilder(
                                                        test.m_WebServiceClient, TEST_JOB_ID)
                                                    .includeInterim()
                                                    .score(10.0);

            Future<Alert> alertFuture = test.m_PollAlertService.longPoll(requestBuilder);

            // Flushing should fire an alert
            InterimResultsTest.test(test.m_WebServiceClient.flushJob(TEST_JOB_ID, true) == true);


            Alert alert = alertFuture.get();
            InterimResultsTest.test(alert.isInterim());
            InterimResultsTest.test(alert.getBucket().isInterim());

            test.m_PollAlertService.shutdown();


            test.verifyFarequoteInterimBuckets(TEST_JOB_ID, false);
            test.verifyFarequoteInterimBuckets(TEST_JOB_ID, true);
            test.verifyFarequoteInterimRecords(TEST_JOB_ID, false);
            test.verifyFarequoteInterimRecords(TEST_JOB_ID, true);
            test.verifyFarequoteInterimInfluencers(TEST_JOB_ID, true);
            test.verifyFarequoteInterimInfluencers(TEST_JOB_ID, false);

            test.verifyInterimResultsAreRecalculated();

            //==========================
            // Clean up test jobs
            InterimResultsTest.test(test.m_WebServiceClient.closeJob(TEST_JOB_ID) == true);
            test.deleteJobs(jobUrls);
        }

        LOGGER.info("All tests passed Ok");
    }
}