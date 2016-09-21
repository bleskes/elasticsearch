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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataCounts;
import com.prelert.job.DataDescription;
import com.prelert.job.Detector;
import com.prelert.job.JobConfiguration;
import com.prelert.job.JobDetails;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.rs.data.MultiDataPostResult;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;

/**
 * Check that after closing and restarting a job (i.e. persisting
 * its state) we get the same results and data count values as
 * running the data without peristing & restoring state.
 *
 * Uses the standard flightcenter data set but it is split
 * into mulitple parts for the first job -closing the job after
 * each part is uploaded. A second job is run with the continuous
 * stream without persisting/restoring state
 *
 * The system property 'prelert.test.data.home' must be set and point
 * to a directory containing the file:
 * <ol>
 * <li>/engine_api_integration_test/flightcentre.csv</li>
 * </ol>
 */
public class RestoreStateTest extends BaseIntegrationTest
{
    private static final String SPLIT_JOB = "split-flightcenter";
    private static final String ONE_SHOT_JOB = "oneshot-flightcenter";

    public RestoreStateTest(String baseUrl)
    {
        super(baseUrl, true);
    }

    @Override
    protected void runTest() throws IOException
    {
        deleteJob(SPLIT_JOB);
        deleteJob(ONE_SHOT_JOB);

        doSplitJob();
        doOneShotJob();
        compareDataCounts();
        compareResults();
    }

    private void createJob(String jobId) throws IOException
    {
        Detector detector = new Detector();
        detector.setFieldName("responsetime");
        detector.setByFieldName("airline");
        List<Detector> d = new ArrayList<>();
        d.add(detector);
        AnalysisConfig config = new AnalysisConfig();
        config.setDetectors(d);
        config.setBucketSpan(3600L);

        DataDescription dd = new DataDescription();
        dd.setTimeField("_time");
        dd.setFieldDelimiter(',');

        JobConfiguration jobConfig = new JobConfiguration(config);
        jobConfig.setId(jobId);
        jobConfig.setDataDescription(dd);

        String backJobId = m_EngineApiClient.createJob(jobConfig);
        if (backJobId == null || backJobId.isEmpty())
        {
            m_Logger.error("No Job Id returned by create job");
            test(jobId != null && jobId.isEmpty() == false);
        }
        test(backJobId.equals(jobId));
    }

    private void doSplitJob() throws IOException
    {
        createJob(SPLIT_JOB);

        File data1 = new File(m_TestDataHome +
                "/engine_api_integration_test/flightcentre.csv");
        BufferedReader reader = new BufferedReader(new FileReader(data1));

        String header = reader.readLine();

        StringBuilder sb = new StringBuilder(header).append("\n");

        // There is about 175,000 lines in the file so do it in 3 chunks
        final int LINES_IN_UPLOAD = 50000;

        // first upload
        for (int i=0; i<LINES_IN_UPLOAD; ++i)
        {
            sb.append(reader.readLine()).append("\n");
        }

        MultiDataPostResult response = m_EngineApiClient.streamingUpload(SPLIT_JOB,
                                            new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8)),
                                            false);

        test(response.anErrorOccurred() == false);
        test(m_EngineApiClient.closeJob(SPLIT_JOB) == true);

        // second upload
        sb.delete(0, sb.length());
        sb.append(header).append("\n");

        for (int i=0; i<LINES_IN_UPLOAD; ++i)
        {
            sb.append(reader.readLine()).append("\n");
        }

        response = m_EngineApiClient.streamingUpload(SPLIT_JOB,
                                            new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8)),
                                            false);

        test(response.anErrorOccurred() == false);
        test(m_EngineApiClient.closeJob(SPLIT_JOB) == true);

        // final upload
        sb.delete(0, sb.length());
        sb.append(header).append("\n");

        String line = reader.readLine();
        while (line != null)
        {
            sb.append(line).append("\n");
            line = reader.readLine();
        }

        response = m_EngineApiClient.streamingUpload(SPLIT_JOB,
                        new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8)),
                        false);

        reader.close();

        test(response.anErrorOccurred() == false);
        test(m_EngineApiClient.closeJob(SPLIT_JOB) == true);
    }

    private void doOneShotJob() throws IOException
    {
        createJob(ONE_SHOT_JOB);

        File data1 = new File(m_TestDataHome +
                "/engine_api_integration_test/flightcentre.csv");

        m_EngineApiClient.fileUpload(ONE_SHOT_JOB, data1, false);
        test(m_EngineApiClient.closeJob(ONE_SHOT_JOB) == true);
    }

    private void compareDataCounts() throws IOException
    {
        SingleDocument<JobDetails> splitJob = m_EngineApiClient.getJob(SPLIT_JOB);
        SingleDocument<JobDetails> oneShotJob = m_EngineApiClient.getJob(ONE_SHOT_JOB);

        test(splitJob.isExists());
        test(oneShotJob.isExists());

        DataCounts oneShotCounts = oneShotJob.getDocument().getCounts();
        DataCounts splitCounts = splitJob.getDocument().getCounts();

        // number of input bytes will be different so don't compare those
        test(oneShotCounts.getBucketCount() != null);
        test(oneShotCounts.getBucketCount().equals(splitCounts.getBucketCount()));
        test(oneShotCounts.getInputRecordCount() == splitCounts.getInputRecordCount());
        test(oneShotCounts.getProcessedRecordCount() == splitCounts.getProcessedRecordCount());
        test(oneShotCounts.getProcessedFieldCount() == splitCounts.getProcessedFieldCount());
        test(oneShotCounts.getLatestRecordTimeStamp() != null);
        test(oneShotCounts.getLatestRecordTimeStamp().equals(splitCounts.getLatestRecordTimeStamp()));
    }


    private void compareResults() throws IOException
    {
        Pagination<Bucket> splitBuckets = m_EngineApiClient
                                    .prepareGetBuckets(SPLIT_JOB)
                                    .normalizedProbabilityThreshold(70)
                                    .take(24)
                                    .expand(true)
                                    .get();
        Pagination<Bucket> oneShotBuckets = m_EngineApiClient
                                    .prepareGetBuckets(ONE_SHOT_JOB)
                                    .normalizedProbabilityThreshold(70)
                                    .take(24)
                                    .expand(true)
                                    .get();

        test(oneShotBuckets.getHitCount() == splitBuckets.getHitCount());

        for (int i=0; i<oneShotBuckets.getHitCount(); i++)
        {
            test(oneShotBuckets.getDocuments().get(i).getRecordCount() ==
                        splitBuckets.getDocuments().get(i).getRecordCount());

            double diff = oneShotBuckets.getDocuments().get(i).getMaxNormalizedProbability() -
                    splitBuckets.getDocuments().get(i).getMaxNormalizedProbability();

            // check that the scores are fairly close
            test(Math.abs(diff) < 0.01);

            for (int j=0; j<oneShotBuckets.getDocuments().get(i).getRecordCount(); ++j)
            {
                AnomalyRecord a = oneShotBuckets.getDocuments().get(i).getRecords().get(j);
                AnomalyRecord b = splitBuckets.getDocuments().get(i).getRecords().get(j);

                diff = a.getAnomalyScore() - b.getAnomalyScore();

                // check that the scores are fairly close
                test(Math.abs(diff) < 0.01);
            }
        }
    }

    public static void main(String[] args) throws IOException
    {
        String baseUrl = API_BASE_URL;
        if (args.length > 0)
        {
            baseUrl = args[0];
        }


        try (RestoreStateTest test = new RestoreStateTest(baseUrl))
        {
            test.runTest();
            test.m_Logger.info("Restore state test passed Ok");
        }

    }
}
