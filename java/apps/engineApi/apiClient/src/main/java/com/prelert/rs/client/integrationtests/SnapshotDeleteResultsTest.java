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
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.Detector;
import com.prelert.job.JobConfiguration;
import com.prelert.job.ModelSnapshot;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;

public class SnapshotDeleteResultsTest extends BaseIntegrationTest
{
    private static final long BUCKET_SPAN = 3600;

    static final String TEST_JOB_ID = "snapshotdeleteresults";

    public SnapshotDeleteResultsTest(String baseUrl)
    {
        super(baseUrl, true);
    }

    @Override
    protected void runTest() throws IOException
    {
        m_Logger.info("Testing Service at " + m_BaseUrl + " for job " + TEST_JOB_ID);

        // Always delete the test job first in case it is hanging around
        // from a previous run
        m_EngineApiClient.deleteJob(TEST_JOB_ID);

        File farequote = new File(m_TestDataHome +
                "/engine_api_integration_test/farequote.csv");

        // Farequote test
        createFarequoteJob();

        // Play straight through, and get results
        playFile(farequote);

        // Get records
        List<AnomalyRecord> allRecords = getRecords().getDocuments();
        List<Bucket> allBuckets = getBuckets().getDocuments();

        // Delete
        m_EngineApiClient.deleteJob(TEST_JOB_ID);

        // Create again
        createFarequoteJob();

        // Play in chunks with resetting
        playChunks(farequote);

        List<AnomalyRecord> snapshotRecords = getRecords().getDocuments();
        List<Bucket> snapshotBuckets = getBuckets().getDocuments();

        // Compare results
        m_Logger.info("Before buckets: " + allBuckets.size() + ", after: " + snapshotBuckets.size());

        test(allBuckets.size() == snapshotBuckets.size());
        for (int i = 0; i < allBuckets.size(); i++)
        {
            Bucket a = allBuckets.get(i);
            Bucket b = snapshotBuckets.get(i);
            test(a.getEpoch() == b.getEpoch());
            test(a.getEventCount() == b.getEventCount());
            test(a.getBucketSpan() == b.getBucketSpan());
            test(a.isInterim() == b.isInterim());
            testDoubles(a.getAnomalyScore(), b.getAnomalyScore());
            testDoubles(a.getMaxNormalizedProbability(), b.getMaxNormalizedProbability());
        }

        m_Logger.info("Before records: " + allRecords.size() + ", after: " + snapshotRecords.size());
        test(allRecords.size() == snapshotRecords.size());

        // Make sure that the main anomaly in this farequote data has the same score
        m_Logger.info("Before score: " + allRecords.get(0).getAnomalyScore());
        m_Logger.info("After score: " + snapshotRecords.get(0).getAnomalyScore());
        testDoubles(allRecords.get(0).getAnomalyScore(), snapshotRecords.get(0).getAnomalyScore());

        test(m_EngineApiClient.closeJob(TEST_JOB_ID) == true);
    }

    public String createFarequoteJob() throws IOException
    {
        Detector d = new Detector();
        d.setFieldName("responsetime");
        d.setByFieldName("airline");

        AnalysisConfig ac = new AnalysisConfig();
        ac.setBucketSpan(BUCKET_SPAN);
        ac.setLatency(0L);
        ac.setOverlappingBuckets(false);
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

        String jobId = m_EngineApiClient.createJob(config);
        if (jobId == null || jobId.isEmpty())
        {
            m_Logger.error("No Job Id returned by create job");
            test(jobId != null && jobId.isEmpty() == false);
        }
        test(jobId.equals(TEST_JOB_ID));

        return jobId;
    }

    public void playFile(File file) throws IOException
    {
        m_EngineApiClient.fileUpload(TEST_JOB_ID, file, false);
        m_EngineApiClient.closeJob(TEST_JOB_ID);
    }

    public Pagination<AnomalyRecord> getRecords() throws IOException
    {
        return m_EngineApiClient.prepareGetRecords(TEST_JOB_ID).take(3000).get();
    }

    public Pagination<Bucket> getBuckets() throws IOException
    {
        return m_EngineApiClient.prepareGetBuckets(TEST_JOB_ID).take(3000).get();
    }

    public void playChunks(File file) throws IOException
    {
        long numLines = 0;
        String header = null;
        try (BufferedReader br = new BufferedReader(new FileReader(file)))
        {
            String line;
            while ((line = br.readLine()) != null) {
                if (numLines++ == 0)
                {
                    header = line;
                }
            }
        }
        catch (IOException ex)
        {
            m_Logger.error(ex.getMessage());
            test(false);
        }

        test(numLines == 86276L);
        test(!header.isEmpty());

        uploadLines(file, null, 0L, 10000L);
        test(m_EngineApiClient.closeJob(TEST_JOB_ID));
        uploadLines(file, header, 10000L, 40000L);
        test(m_EngineApiClient.closeJob(TEST_JOB_ID));
        uploadLines(file, header, 40000L, 60000L);
        test(m_EngineApiClient.closeJob(TEST_JOB_ID));
        uploadLines(file, header, 60000L, 86276L);
        test(m_EngineApiClient.closeJob(TEST_JOB_ID));

        restoreTo(1359631720000L);

        uploadLines(file, header, 60000L, 86276L);
        test(m_EngineApiClient.closeJob(TEST_JOB_ID));

        restoreTo(1359533646000L);
        uploadLines(file, header, 40000L, 50000L);
        test(m_EngineApiClient.closeJob(TEST_JOB_ID));

        restoreTo(1359380393000L);
        uploadLines(file, header, 10000L, 86276L);
        test(m_EngineApiClient.closeJob(TEST_JOB_ID));
    }

    private void restoreTo(Long lastRecordTime) throws JsonParseException, JsonMappingException, IOException
    {
        String url = m_BaseUrl + "/modelsnapshots/" + TEST_JOB_ID;
        Pagination<ModelSnapshot> pagedSnapshots = m_EngineApiClient.get(url, new TypeReference<Pagination<ModelSnapshot>>() {}, true);
        List<ModelSnapshot> snapshots = pagedSnapshots.getDocuments();
        m_Logger.info("Asking for snapshot at time " + lastRecordTime);

        for (ModelSnapshot ss : snapshots)
        {
            if (ss.getLatestRecordTimeStamp().getTime() == lastRecordTime)
            {
                String fullUrl = url + "/revert?deleteInterveningResults=true&snapshotId=" + ss.getSnapshotId();
                SingleDocument<ModelSnapshot> restored = m_EngineApiClient.post(fullUrl, new TypeReference<SingleDocument<ModelSnapshot>>() {});
                test(restored.isExists());
                return;
            }
        }
        test(false);
    }

    private void uploadLines(File file, String header, Long startLine, Long endLine) throws IOException
    {
        StringBuffer sb = new StringBuffer();
        Long numLines = 0L;
        boolean firstRecord = true;
        if (header != null)
        {
            sb.append(header);
            sb.append("\n");
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file)))
        {
            String line;
            while ((line = br.readLine()) != null && numLines < endLine)
            {
                if (numLines >= startLine)
                {
                    if (firstRecord)
                    {
                        m_Logger.debug("UploadLines, starting with " + line);
                        firstRecord = false;
                    }
                    sb.append(line);
                    sb.append("\n");
                }
                numLines++;
            }
        }
        catch (IOException ex)
        {
            m_Logger.error(ex.getMessage());
            test(false);
        }
        m_Logger.info("About to upload bytes: " + sb.length());
        BufferedInputStream is = new BufferedInputStream(new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8)));
        m_EngineApiClient.streamingUpload(TEST_JOB_ID, is, false);
    }

    public void testDoubles(double a, double b)
    throws IllegalStateException
    {
        if (Math.abs(a - b) / Math.max(0.000000001, Math.min(a, b)) > 0.05)
        {
            m_Logger.info("Doubles not equal: " + a + " , " + b);
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
        String baseUrl = API_BASE_URL;
        if (args.length > 0)
        {
            baseUrl = args[0];
        }


        try (SnapshotDeleteResultsTest test = new SnapshotDeleteResultsTest(baseUrl))
        {
            test.runTest();
            test.m_Logger.info("All tests passed Ok");
        }

    }
}
