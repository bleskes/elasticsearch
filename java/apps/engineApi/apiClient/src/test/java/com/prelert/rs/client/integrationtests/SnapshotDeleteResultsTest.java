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
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

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
import com.prelert.rs.client.EngineApiClient;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;

public class SnapshotDeleteResultsTest implements Closeable
{
    private static final Logger LOGGER = Logger.getLogger(SnapshotDeleteResultsTest.class);

    private static final long BUCKET_SPAN = 3600;

    static final String TEST_JOB_ID = "snapshotdeleteresults";

    /**
     * The default base Url used in the test
     */
    public static final String API_BASE_URL = "http://localhost:8080/engine/v2";

    private final EngineApiClient m_WebServiceClient;
    private final String m_BaseUrl;

    /**
     * Creates a new http client call {@linkplain #close()} once finished
     */
    public SnapshotDeleteResultsTest(String baseUrl)
    {
        m_WebServiceClient = new EngineApiClient(baseUrl);
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

        String jobId = m_WebServiceClient.createJob(config);
        if (jobId == null || jobId.isEmpty())
        {
            LOGGER.error("No Job Id returned by create job");
            test(jobId != null && jobId.isEmpty() == false);
        }
        test(jobId.equals(TEST_JOB_ID));

        return jobId;
    }

    public void playFile(File file) throws IOException
    {
        m_WebServiceClient.fileUpload(TEST_JOB_ID, file, false);
        m_WebServiceClient.closeJob(TEST_JOB_ID);
    }

    public Pagination<AnomalyRecord> getRecords() throws IOException
    {
        return m_WebServiceClient.prepareGetRecords(TEST_JOB_ID).take(3000).get();
    }

    public Pagination<Bucket> getBuckets() throws IOException
    {
        return m_WebServiceClient.prepareGetBuckets(TEST_JOB_ID).take(3000).get();
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
            LOGGER.error(ex.getMessage());
            test(false);
        }

        test(numLines == 86276L);
        test(!header.isEmpty());

        uploadLines(file, null, 0L, 10000L);
        test(m_WebServiceClient.closeJob(TEST_JOB_ID));
        uploadLines(file, header, 10000L, 40000L);
        test(m_WebServiceClient.closeJob(TEST_JOB_ID));
        uploadLines(file, header, 40000L, 60000L);
        test(m_WebServiceClient.closeJob(TEST_JOB_ID));
        uploadLines(file, header, 60000L, 86276L);
        test(m_WebServiceClient.closeJob(TEST_JOB_ID));

        restoreTo(1359631720000L);

        uploadLines(file, header, 60000L, 86276L);
        test(m_WebServiceClient.closeJob(TEST_JOB_ID));

        restoreTo(1359533646000L);
        uploadLines(file, header, 40000L, 50000L);
        test(m_WebServiceClient.closeJob(TEST_JOB_ID));

        restoreTo(1359380393000L);
        uploadLines(file, header, 10000L, 86276L);
        test(m_WebServiceClient.closeJob(TEST_JOB_ID));
    }

    private void restoreTo(Long lastRecordTime) throws JsonParseException, JsonMappingException, IOException
    {
        String url = m_BaseUrl + "/modelsnapshots/" + TEST_JOB_ID;
        Pagination<ModelSnapshot> pagedSnapshots = m_WebServiceClient.get(url, new TypeReference<Pagination<ModelSnapshot>>() {}, true);
        List<ModelSnapshot> snapshots = pagedSnapshots.getDocuments();
        LOGGER.info("Asking for snapshot at time " + lastRecordTime);

        for (ModelSnapshot ss : snapshots)
        {
            if (ss.getLatestRecordTimeStamp().getTime() == lastRecordTime)
            {
                String fullUrl = url + "/revert?deleteInterveningResults=true&snapshotId=" + ss.getSnapshotId();
                SingleDocument<ModelSnapshot> restored = m_WebServiceClient.post(fullUrl, new TypeReference<SingleDocument<ModelSnapshot>>() {});
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
                        LOGGER.debug("UploadLines, starting with " + line);
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
            LOGGER.error(ex.getMessage());
            test(false);
        }
        LOGGER.info("About to upload bytes: " + sb.length());
        BufferedInputStream is = new BufferedInputStream(new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8)));
        m_WebServiceClient.streamingUpload(TEST_JOB_ID, is, false);
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

    public static void testDoubles(double a, double b)
    throws IllegalStateException
    {
        if (Math.abs(a - b) / Math.max(0.000000001, Math.min(a, b)) > 0.05)
        {
            LOGGER.info("Doubles not equal: " + a + " , " + b);
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

        LOGGER.info("Testing Service at " + baseUrl + " for job " + TEST_JOB_ID);

        final String prelertTestDataHome = System.getProperty("prelert.test.data.home");
        if (prelertTestDataHome == null)
        {
            throw new IllegalStateException("Error property prelert.test.data.home is not set");
        }

        try (SnapshotDeleteResultsTest test = new SnapshotDeleteResultsTest(baseUrl))
        {
            // Always delete the test job first in case it is hanging around
            // from a previous run
            test.m_WebServiceClient.deleteJob(TEST_JOB_ID);

            File farequote = new File(prelertTestDataHome +
                    "/engine_api_integration_test/farequote.csv");

            // Farequote test
            test.createFarequoteJob();

            // Play straight through, and get results
            test.playFile(farequote);

            // Get records
            List<AnomalyRecord> allRecords = test.getRecords().getDocuments();
            List<Bucket> allBuckets = test.getBuckets().getDocuments();

            // Delete
            test.m_WebServiceClient.deleteJob(TEST_JOB_ID);

            // Create again
            test.createFarequoteJob();

            // Play in chunks with resetting
            test.playChunks(farequote);

            List<AnomalyRecord> snapshotRecords = test.getRecords().getDocuments();
            List<Bucket> snapshotBuckets = test.getBuckets().getDocuments();

            // Compare results
            LOGGER.info("Before buckets: " + allBuckets.size() + ", after: " + snapshotBuckets.size());

            SnapshotDeleteResultsTest.test(allBuckets.size() == snapshotBuckets.size());
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

            LOGGER.info("Before records: " + allRecords.size() + ", after: " + snapshotRecords.size());
            SnapshotDeleteResultsTest.test(allRecords.size() == snapshotRecords.size());

            // Make sure that the main anomaly in this farequote data has the same score
            LOGGER.info("Before score: " + allRecords.get(0).getAnomalyScore());
            LOGGER.info("After score: " + snapshotRecords.get(0).getAnomalyScore());
            testDoubles(allRecords.get(0).getAnomalyScore(), snapshotRecords.get(0).getAnomalyScore());

            test(test.m_WebServiceClient.closeJob(TEST_JOB_ID) == true);
        }

        LOGGER.info("All tests passed Ok");
    }
}
