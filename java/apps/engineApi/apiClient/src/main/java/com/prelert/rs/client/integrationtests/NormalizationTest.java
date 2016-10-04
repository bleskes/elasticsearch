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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.Detector;
import com.prelert.job.JobConfiguration;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.rs.data.Pagination;


/**
 * Tests the normalisation of the API buckets and records endpoints.
 * Does not validate the actual normalised values other than to say
 * that for a given batch job at least 1 anomaly should be 100%.
 * <br>
 * The system property 'prelert.test.data.home' must be set and point
 * to a directory containing the file:
 * <ol>
 * <li>/engine_api_integration_test/farequote.csv</li>
 * </ol>
 *
 */
public class NormalizationTest extends BaseIntegrationTest
{
    static final long FAREQUOTE_NUM_BUCKETS = 1439;

    static final String TEST_FAREQUOTE = "farequote-norm-test";
    static final String TEST_FAREQUOTE_NO_RENORMALIZATION = "farequote-no-renormalization-test";

    public static final String ES_BASE_URL = "http://localhost:9200/";


    private String m_EsBaseUrl;

    /**
     * Creates a new http client call {@linkplain #close()} once finished
     */
    public NormalizationTest(String baseUrl, String esBaseUrl)
    {
        super(baseUrl, true);
        m_EsBaseUrl = esBaseUrl;
    }

    @Override
    protected void runTest() throws IOException
    {
        List<String> jobUrls = new ArrayList<>();
        File fareQuoteData = new File(m_TestDataHome + "/engine_api_integration_test/farequote.csv");

        // Always delete the test job first in case it is hanging around
        // from a previous run

        // Farequote test
        m_EngineApiClient.deleteJob(TEST_FAREQUOTE);
        jobUrls.add(TEST_FAREQUOTE);
        createFarequoteJob();
        m_EngineApiClient.fileUpload(TEST_FAREQUOTE, fareQuoteData, false);
        m_EngineApiClient.closeJob(TEST_FAREQUOTE);

        verifyFarequoteNormalisedBuckets(TEST_FAREQUOTE);
        verifyFarequoteNormalisedRecords(TEST_FAREQUOTE);

        // Farequote no renormalization test
        jobUrls.add(TEST_FAREQUOTE_NO_RENORMALIZATION);
        m_EngineApiClient.deleteJob(TEST_FAREQUOTE_NO_RENORMALIZATION);
        createFarequoteNoRenormalizationJob();
        m_EngineApiClient.fileUpload(TEST_FAREQUOTE_NO_RENORMALIZATION, fareQuoteData, false);
        m_EngineApiClient.closeJob(TEST_FAREQUOTE_NO_RENORMALIZATION);

        verifyNormalizedScoresAreEqualToInitialScores(TEST_FAREQUOTE_NO_RENORMALIZATION, m_EsBaseUrl);

        //==========================
        // Clean up test jobs
        deleteJobs(jobUrls);
    }

    public String createFarequoteJob() throws IOException
    {
        return createFarequoteJob(TEST_FAREQUOTE, null);
    }

    public String createFarequoteNoRenormalizationJob() throws IOException
    {
        return createFarequoteJob(TEST_FAREQUOTE_NO_RENORMALIZATION, 0L);
    }

    private String createFarequoteJob(String jobId, Long renormalizationWindowDays)
            throws IOException
    {
        Detector d = new Detector();
        d.setFunction("metric");
        d.setFieldName("responsetime");
        d.setByFieldName("airline");

        AnalysisConfig ac = new AnalysisConfig();
        ac.setBucketSpan(300L);
        ac.setOverlappingBuckets(false);
        ac.setDetectors(Arrays.asList(d));

        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.DELIMITED);
        dd.setFieldDelimiter(',');
        dd.setTimeField("time");
        dd.setTimeFormat("yyyy-MM-dd HH:mm:ssX");

        JobConfiguration config = new JobConfiguration(ac);
        config.setDescription("Farequote normalisation test");
        config.setId(jobId);
        config.setDataDescription(dd);
        config.setRenormalizationWindowDays(renormalizationWindowDays);

        test(jobId.equals(m_EngineApiClient.createJob(config)));
        return jobId;
    }

    /**
     * Test the system change normalisation for the farequote
     * data set.
     * <ol>
     * <li>Test that getting all the results at once is the same as
     * paging them</li>
     * <li>Check there is one large anomaly in the data with score 100</li>
     * <li>Get buckets by date range and check the values match the same
     * buckets from get all results.</li>
     * </ol>
     *
     *
     * @param jobId
     * @return
     * @throws IOException
     */
    public boolean verifyFarequoteNormalisedBuckets(String jobId)
    throws IOException
    {
        Pagination<Bucket> allBuckets = m_EngineApiClient.prepareGetBuckets(jobId).take(1500).get();
        test(allBuckets.getDocumentCount() == FAREQUOTE_NUM_BUCKETS);
        test(allBuckets.getHitCount() == FAREQUOTE_NUM_BUCKETS);


        /*
         * Test that getting all the results at once is the same as
         * paging them.
         */
        List<Bucket> pagedBuckets = new ArrayList<>();
        long skip = 0, take = 100;
        while (skip < FAREQUOTE_NUM_BUCKETS)
        {
            Pagination<Bucket> buckets = m_EngineApiClient.prepareGetBuckets(jobId).skip(skip)
                    .take(take).get();
            pagedBuckets.addAll(buckets.getDocuments());

            skip += take;
        }
        test(pagedBuckets.size() == FAREQUOTE_NUM_BUCKETS);

        for (int i=0; i<FAREQUOTE_NUM_BUCKETS; ++i)
        {
            test(Double.compare(pagedBuckets.get(i).getAnomalyScore(),
                                allBuckets.getDocuments().get(i).getAnomalyScore()) == 0);

            test(Double.compare(pagedBuckets.get(i).getMaxNormalizedProbability(),
                    allBuckets.getDocuments().get(i).getMaxNormalizedProbability()) == 0);

            test(pagedBuckets.get(i).equals(allBuckets.getDocuments().get(i)));
        }


        /*
         * Test that paging buckets by date is the same
         */
        pagedBuckets = new ArrayList<>();
        skip = 0; take = 140;
        while (skip < allBuckets.getHitCount())
        {
            Pagination<Bucket> buckets = m_EngineApiClient.prepareGetBuckets(jobId)
                    .skip(skip).take(take)
                    .start(allBuckets.getDocuments().get(0).getEpoch())
                    .end(allBuckets.getDocuments().get((int)allBuckets.getHitCount()-1).getEpoch() +1)
                    .get();

            pagedBuckets.addAll(buckets.getDocuments());

            skip += take;
        }
        test(pagedBuckets.size() == allBuckets.getDocumentCount());

        for (int i=0; i<pagedBuckets.size(); ++i)
        {
            test(Double.compare(pagedBuckets.get(i).getAnomalyScore(),
                                allBuckets.getDocuments().get(i).getAnomalyScore()) == 0);

            test(Double.compare(pagedBuckets.get(i).getMaxNormalizedProbability(),
                    allBuckets.getDocuments().get(i).getMaxNormalizedProbability()) == 0);

            test(pagedBuckets.get(i).equals(allBuckets.getDocuments().get(i)));
        }

        /*
         * Test get buckets by date range with a time string
         */
        String [] startDateFormats = new String[] {"2013-01-30T15:10:00Z", "1359558600"};
        String [] endDateFormats = new String[] {"2013-01-31T22:10:00.000+00:00", "1359670200"};

        for (int i=0; i<startDateFormats.length; ++i)
        {
            Pagination<Bucket> byDate = m_EngineApiClient.prepareGetBuckets(jobId)
                    .take(1000)
                    .start(startDateFormats[i])
                    .end(endDateFormats[i]).get();

            test(byDate.getDocuments().get(0).getEpoch() == 1359558600l);
            test(byDate.getDocuments().get(byDate.getDocumentCount() -1).getEpoch() == 1359669900l);

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
            for (int j=0; j<byDate.getDocumentCount(); ++j)
            {
                test(Double.compare(byDate.getDocuments().get(j).getAnomalyScore(),
                        allBuckets.getDocuments().get(j + startIndex).getAnomalyScore()) == 0);

                test(Double.compare(pagedBuckets.get(i).getMaxNormalizedProbability(),
                        allBuckets.getDocuments().get(i).getMaxNormalizedProbability()) == 0);

                test(byDate.getDocuments().get(j).equals(allBuckets.getDocuments().get(j + startIndex)));
            }
        }

        /*
         * We know there is only one large anomaly in the farequote data
         * with score 80+, and it spans two buckets
         */
        int highAnomalyScoreCount = 0;
        for (Bucket b : pagedBuckets)
        {
            if (b.getAnomalyScore() >= 65.0)
            {
                ++highAnomalyScoreCount;
            }
        }
        test(highAnomalyScoreCount == 2);

        // The big anomaly spans buckets 771 and 772.  Which one of these has
        // the higher score may vary as the analytics are updated, but the
        // sum should be at least 155.
        double bucket771Score = pagedBuckets.get(770).getAnomalyScore();
        double bucket772Score = pagedBuckets.get(771).getAnomalyScore();
        test(bucket771Score + bucket772Score >= 155.0);

        Pagination<Bucket> allBucketsExpanded = m_EngineApiClient.prepareGetBuckets(jobId)
                .expand(true).take(1500).get();

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
                test(r.getAnomalyScore() == bucket.getAnomalyScore());
                bucketMax = Math.max(r.getNormalizedProbability(), bucketMax);
            }

            test(bucketMax == bucket.getMaxNormalizedProbability());
        }

        return true;
    }

    /**
     * Get records via the 'records' end point and check the normalised
     * scores for both unusual & state change.
     *
     * @param baseUrl
     * @param jobId
     * @return
     * @throws IOException
     */
    public boolean verifyFarequoteNormalisedRecords(String jobId)
    throws IOException
    {
        Pagination<AnomalyRecord> allRecords = m_EngineApiClient.prepareGetRecords(jobId)
                .take(3000).get();

        /*
         * Test that getting all the results at once is the same as
         * paging them.
         */
        List<AnomalyRecord> pagedRecords = new ArrayList<>();
        long skip = 0, take = 1000;

        Pagination<AnomalyRecord> page = m_EngineApiClient.prepareGetRecords(jobId).skip(skip)
                .take(take).get();
        skip += take;
        pagedRecords.addAll(page.getDocuments());

        while (skip < page.getHitCount())
        {
            page = m_EngineApiClient.prepareGetRecords(jobId).skip(skip).take(take).get();
            skip += take;
            pagedRecords.addAll(page.getDocuments());
        }

        int recordIndex = 0;
        for (AnomalyRecord record : allRecords.getDocuments())
        {
            test(record.equals(pagedRecords.get(recordIndex)));
            ++recordIndex;
        }
        test(recordIndex == pagedRecords.size());

        /*
         * Test paging by date is the same as getting them all
         * at once
         */

        // need start and end dates first
        Pagination<Bucket> allBuckets = m_EngineApiClient.prepareGetBuckets(jobId).expand(true)
                .take(3000).get();
        long startDate = allBuckets.getDocuments().get(0).getEpoch();
        long endDate = allBuckets.getDocuments().get(allBuckets.getDocumentCount()-1).getEpoch() + 1;

        pagedRecords = new ArrayList<>();
        skip = 0; take = 200;


        page = m_EngineApiClient.prepareGetRecords(jobId).skip(skip).take(take).start(startDate)
                .end(endDate).get();
        skip += take;
        pagedRecords.addAll(page.getDocuments());

        while (skip < page.getHitCount())
        {
            page = m_EngineApiClient.prepareGetRecords(jobId).skip(skip).take(take)
                    .start(startDate).end(endDate).get();

            skip += take;
            pagedRecords.addAll(page.getDocuments());
        }

        recordIndex = 0;
        test(allRecords.getHitCount() == pagedRecords.size());

        for (AnomalyRecord record : allRecords.getDocuments())
        {
            test(record.equals(pagedRecords.get(recordIndex)));
            ++recordIndex;
        }
        test(recordIndex == pagedRecords.size());


        /*
         * There should be at least two anomalies with unusual score of 80+
         * and at least two with anomaly score 65+
         */
        int highAnomalyScoreCount = 0;
        int highUnusualScoreCount = 0;
        for (AnomalyRecord record : pagedRecords)
        {
            if (record.getNormalizedProbability() >= 80.0)
            {
                ++highUnusualScoreCount;
            }
            if (record.getAnomalyScore() >= 65.0)
            {
                ++highAnomalyScoreCount;
            }
        }

        test(highUnusualScoreCount >= 2);
        test(highAnomalyScoreCount >= 2);

        return true;
    }

    public boolean verifyNormalizedScoresAreEqualToInitialScores(String jobId, String esBaseUrl) throws IOException
    {
        try (ElasticsearchDirectClient directClient = new ElasticsearchDirectClient(
                esBaseUrl + "prelertresults-" + jobId + "/"))
        {
            Pagination<Bucket> allBuckets = m_EngineApiClient.prepareGetBuckets(jobId)
                    .take(1500L).expand(true).get();
            for (Bucket bucket : allBuckets.getDocuments())
            {
                double normalizedScore = bucket.getAnomalyScore();
                double initialScore = directClient.getBucketInitialScore(bucket.getTimestamp());
                test(normalizedScore == initialScore);
            }
            return true;
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
        String baseUrl = API_BASE_URL;
        if (args.length > 0)
        {
            baseUrl = args[0];
        }

        String esBaseUrl = ES_BASE_URL;
        if (args.length > 1)
        {
            esBaseUrl = args[1];
        }
        if (!esBaseUrl.endsWith("/"))
        {
            esBaseUrl += "/";
        }

        try (NormalizationTest test = new NormalizationTest(baseUrl, esBaseUrl))
        {
            test.runTest();
            test.m_Logger.info("All tests passed Ok");
        }

    }
}
