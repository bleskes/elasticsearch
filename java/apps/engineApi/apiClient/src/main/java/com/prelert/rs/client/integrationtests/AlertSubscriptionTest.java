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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.Detector;
import com.prelert.job.JobConfiguration;
import com.prelert.job.alert.Alert;
import com.prelert.job.alert.AlertType;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.rs.client.AlertRequestBuilder;
import com.prelert.rs.client.EngineApiClient;
import com.prelert.rs.data.ApiError;
import com.prelert.rs.data.SingleDocument;


/**
 * Tests the long poll alert subscription end point.
 * <br>Jobs are created and an alert is subscribed for each then data
 * is uploaded to the job. Some alerts are expected to fire others
 * will timeout.
 * Synchronising registering for an alert and data upload is little difficult
 * especially on machines with few cores so the the tests are run
 * sequentially (apart from one where two jobs are run simultaneously).
 * Without rewriting the API Client the fine grain sychnronisation control
 * needed to make this test deterministic is not available - be aware the
 * test will probably fail on single core machines.
 *
 * <br>Returns a non-zero value if the tests fail.
 */
public class AlertSubscriptionTest
{
    private static final Logger LOGGER = Logger.getLogger(AlertSubscriptionTest.class);

    /**
     * The default base Url used in the test
     */
    public static final String API_BASE_URL = "http://localhost:8080/engine/v2";

    public static final String ALERTING_JOB_1 = "alerting-job-1";
    public static final String ALERTING_JOB_2 = "alerting-job-2";
    public static final String ALERTING_JOB_3 = "alerting-job-3";
    public static final String ALERTING_JOB_3_B = "alerting-job-3-b";
    public static final String ALERT_TIMEOUT_JOB = "alerting-timeout-job";

    public static final String [] JOB_IDS = {ALERTING_JOB_1, ALERTING_JOB_2, ALERTING_JOB_3,
        ALERTING_JOB_3_B, ALERT_TIMEOUT_JOB};


    private List<URI> m_BucketUris;


    public AlertSubscriptionTest()
    {
        m_BucketUris = Collections.synchronizedList(new ArrayList<URI>());
    }

    private void setupJobs(EngineApiClient client) throws IOException
    {
        for (String s : JOB_IDS)
        {
            client.deleteJob(s);
        }

        Detector d1 = new Detector();
        d1.setFieldName("In Discards");
        d1.setByFieldName("host");
        Detector d2 = new Detector();
        d2.setFieldName("In Octets");
        d2.setByFieldName("host");
        Detector d3 = new Detector();
        d3.setFieldName("Out Discards");
        d3.setByFieldName("host");
        Detector d4 = new Detector();
        d4.setFieldName("Out Octets");
        d4.setByFieldName("host");

        AnalysisConfig ac = new AnalysisConfig();
        ac.setBucketSpan(3600L);
        ac.setDetectors(Arrays.asList(d1, d2, d3, d4));

        DataDescription dd = new DataDescription();
        dd.setFieldDelimiter(',');
        dd.setTimeField("time");
        dd.setTimeFormat("EEE MM/dd/yyyy HH:mm");

        JobConfiguration config = new JobConfiguration();
        config.setAnalysisConfig(ac);
        config.setDataDescription(dd);

        for (String s : JOB_IDS)
        {
            config.setId(s);
            String jobId = client.createJob(config);
            test(jobId.equals(s));
        }
    }

    private static void configureLogging()
    {
        // configure log4j
        ConsoleAppender console = new ConsoleAppender();
        console.setLayout(new PatternLayout("%d [%p|%c|%C{1}] %m%n"));
        console.setThreshold(Level.INFO);
        console.activateOptions();
        Logger.getRootLogger().addAppender(console);
    }


    /**
     * Check that the alert's bucket URL points to a bucket and that
     * bucket has records
     *
     * @param uri
     * @param minAnomalyScore
     * @return
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    public boolean validateBucketUrl(EngineApiClient client, URI uri, double minAnomalyScore)
    throws JsonParseException, JsonMappingException, IOException
    {
        test(uri != null);
        SingleDocument<Bucket> bucket = client.get(uri, new TypeReference<SingleDocument<Bucket>>() {});
        test(bucket != null);
        test(bucket.getDocument() != null);
        test(bucket.getDocument().getRecords().isEmpty() == false);
        test(bucket.getDocument().getRecords().size() == bucket.getDocument().getRecordCount());

        // Because of re-normalisation the anomaly score might not be what it
        // was when the alert was triggered
        //test(bucket.getDocument().getAnomalyScore() >= minAnomalyScore);

        return true;
    }


    /**
     * Throws if <code>condition</code> if false
     *
     * @param condition
     * @throws IllegalStateException
     */
    public void test(boolean condition)
    throws IllegalStateException
    {
        if (condition == false)
        {
            throw new IllegalStateException();
        }
    }


    /**
     * Subscribe to the long poll alerts end point for the job.
     * When the alert is fired check its values and if it timed
     * out or not.
     */
    private class LongPollAlertTest implements Runnable
    {
        EngineApiClient m_Client;
        String m_JobId;
        Double m_ScoreThreshold;
        Double m_ProbabilityThreshold;
        boolean m_ShouldTimeout;
        volatile boolean m_Quit;
        boolean m_TestPassed;
        CountDownLatch m_Latch;


        public LongPollAlertTest(EngineApiClient client, String jobId,
                Double scoreThreshold, Double probabilityThreshold, boolean shouldTimeout,
                CountDownLatch latch)
        {
            m_Client = client;
            m_JobId = jobId;
            m_ScoreThreshold = scoreThreshold;
            m_ProbabilityThreshold = probabilityThreshold;
            m_ShouldTimeout = shouldTimeout;
            m_Latch = latch;
        }

        /**
         * Stops the thread's run loop
         */
        public void quit()
        {
            m_Quit = true;
        }

        /**
         * Tests passed
         * @return
         */
        public boolean isTestPassed()
        {
            return m_TestPassed;
        }

        private Alert getAlert(long timeout) throws IOException
        {
            AlertRequestBuilder builder = new AlertRequestBuilder(m_Client, m_JobId).timeout(timeout);

            if (m_ScoreThreshold != null)
            {
                builder.score(m_ScoreThreshold);
            }
            if (m_ProbabilityThreshold != null)
            {
                builder.probability(m_ProbabilityThreshold);
            }

            return builder.get();
        }

        @Override
        public void run()
        {
            try
            {
                Alert alert = getAlert(3);


                // This alert will timeout as the upload thread is still waiting
                // on the latch
                test(alert.isTimeout());
                test(alert.getJobId().equals(m_JobId));
                test(alert.getRecords() == null);
                test(alert.getBucket() == null);

                m_Latch.countDown();
                try
                {
                    m_Latch.await();
                }
                catch (InterruptedException e1)
                {
                    LOGGER.error(e1);
                    test(false);
                }

                final int TIMEOUT = 30;
                alert = getAlert(TIMEOUT);


                // may get errors about the job not running if the data
                // upload hasn't started
                while (alert == null && m_Client.getLastError() != null)
                {
                    ApiError err = m_Client.getLastError();
                    test(err.getErrorCode() == ErrorCodes.JOB_NOT_RUNNING);

                    alert = getAlert(TIMEOUT);

                    if (m_Quit)
                    {
                        LOGGER.warn("Quitting the alert poll thread");
                        break;
                    }

                    try
                    {
                        Thread.sleep(200);
                    }
                    catch (InterruptedException e)
                    {
                        LOGGER.error(e);
                        break;
                    }
                }

                // alert should exist at this point
                test(alert != null);
                test(alert.getJobId().equals(m_JobId));
                test(alert.isTimeout() == m_ShouldTimeout);
                test(alert.isInterim() == false);
                test(alert.getAlertType() == AlertType.BUCKET);

                if (alert.isTimeout() == false)
                {
                    if (m_ScoreThreshold != null && m_ProbabilityThreshold != null)
                    {
                        test(alert.getAnomalyScore() >= m_ScoreThreshold ||
                                alert.getMaxNormalizedProbability() >= m_ProbabilityThreshold);
                    }
                    else if (m_ScoreThreshold != null)
                    {
                        test(alert.getAnomalyScore() >= m_ScoreThreshold);
                        test(alert.getRecords() == null);
                        test(alert.getBucket() != null);
                        test(alert.getBucket().getTimestamp() != null);
                        test(alert.getBucket().getAnomalyScore() >= m_ScoreThreshold);
                    }
                    else if (m_ProbabilityThreshold != null)
                    {
                        test(alert.getMaxNormalizedProbability() >= m_ProbabilityThreshold);
                        test(alert.getBucket() == null);
                        test(alert.getRecords() != null);
                        test(alert.getRecords().size() > 0);
                        for (AnomalyRecord r : alert.getRecords())
                        {
                            test(r.getNormalizedProbability() >= m_ProbabilityThreshold);
                            test(r.getTimestamp() != null);
                        }
                    }

                    m_BucketUris.add(alert.getUri());

                    LOGGER.info("Got alert for job " + m_JobId);
                }
                else
                {
                    LOGGER.info("Alert timed out for " + m_JobId);
                }

                m_TestPassed = true;
            }
            catch (JsonParseException e)
            {
                e.printStackTrace();
                test(false);
            }
            catch (JsonMappingException e)
            {
                e.printStackTrace();
                test(false);
            }
            catch (IOException e)
            {
                e.printStackTrace();
                test(false);
            }
        }
    }

    /**
     * Upload the contents of a file to the Engine API
     * and closes the job.
     * <br>The initiateUpload() upload function writes the csv
     * header so the job will be started. run() then writes the
     * rest of the file.
     *
     */
    private class DataUploader implements Runnable
    {
        EngineApiClient m_Client;
        String m_JobId;
        File m_File;
        CountDownLatch m_Latch;

        public DataUploader(EngineApiClient client, String jobId, File file, CountDownLatch latch)
        throws IOException
        {
            m_Client = client;
            m_JobId = jobId;
            m_File = file;
            m_Latch = latch;
        }

        /**
         * Uploads the beginning of the data to the job causing the job
         * to become active
         */
        public void initiateUpload()
        {
            try
            {
                BufferedReader reader = new BufferedReader(new FileReader(m_File));
                String header = reader.readLine();

                ByteArrayInputStream is = new ByteArrayInputStream(header.getBytes(StandardCharsets.UTF_8));
                m_Client.streamingUpload(m_JobId, is, false);
                reader.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
                test(false);
            }
        }

        @Override
        public void run()
        {
            try
            {
                m_Latch.countDown();
                m_Latch.await();

                slowUpload(m_JobId, m_File, 200);
                m_Client.closeJob(m_JobId);
            }
            catch (IOException | InterruptedException e)
            {
                LOGGER.error(e);
                test(false);
            }
            finally
            {
                try
                {
                    m_Client.close();
                } catch (IOException e)
                {
                    LOGGER.error("Failed to close client", e);
                }
            }
        }

        public void slowUpload(String jobId, final File dataFile, final long sleepTimeMs)
        throws IOException
        {
            final PipedInputStream pipedIn = new PipedInputStream();
            final PipedOutputStream pipedOut = new PipedOutputStream(pipedIn);

            new Thread(new Runnable() {
                @Override
                public void run()
                {
                    int n;
                    byte [] buf = new byte[2048];
                    try
                    {
                        FileInputStream fs;
                        try
                        {
                            fs = new FileInputStream(dataFile);
                        }
                        catch (FileNotFoundException e)
                        {
                            e.printStackTrace();
                            return;
                        }

                        while((n = fs.read(buf)) > -1)
                        {
                            pipedOut.write(buf, 0, n);
                            Thread.sleep(sleepTimeMs);
                        }
                        fs.close();

                        pipedOut.close();
                    }
                    catch (IOException e)
                    {
                        LOGGER.info(e);
                    }
                    catch (InterruptedException e)
                    {
                        LOGGER.info(e);
                    }
                }
            }).start();

            m_Client.streamingUpload(jobId, pipedIn, false);
        }
    }


    public void runTests(String baseUrl, String prelertTestDataHome) throws IOException,
            InterruptedException
    {
        try (EngineApiClient client = new EngineApiClient(baseUrl))
        {
            setupJobs(client);

            Alert alert = new AlertRequestBuilder(client, "non-existing-job").timeout(5).get();
            test(alert == null);
            ApiError error = client.getLastError();
            test(error != null);
            test(error.getErrorCode() == ErrorCodes.INVALID_THRESHOLD_ARGUMENT);

            alert = new AlertRequestBuilder(client, "non-existing-job").timeout(5).score(10.0).get();
            test(alert == null);
            error = client.getLastError();
            test(error != null);
            test(error.getErrorCode() == ErrorCodes.MISSING_JOB_ERROR);

            alert = new AlertRequestBuilder(client, ALERTING_JOB_1).timeout(5).score(10.0).get();
            test(alert == null);
            error = client.getLastError();
            test(error != null);
            test(error.getErrorCode() == ErrorCodes.JOB_NOT_RUNNING);

            boolean passed = true;
            for (String jobId : JOB_IDS)
            {
                if (jobId == ALERTING_JOB_3_B)
                {
                    // this job is run in paralled with ALERTING_JOB_3
                    continue;
                }
                List<Thread> uploaderThreads = new ArrayList<>();
                List<LongPollAlertTest> longPollTests = new ArrayList<>();
                List<Thread> alertTestThreads = new ArrayList<>();

                File networkDataFile = new File(prelertTestDataHome +
                        "/engine_api_integration_test/network.csv");

                CountDownLatch latch = new CountDownLatch(2);

                DataUploader dl = new DataUploader(new EngineApiClient(baseUrl), jobId, networkDataFile,
                        latch);
                dl.initiateUpload(); // makes the job active
                uploaderThreads.add(new Thread(dl));

                LongPollAlertTest longPoll;
                if (jobId == ALERT_TIMEOUT_JOB)
                {
                    // this one will timeout
                    longPoll = new LongPollAlertTest(client, jobId, 99.9, 99.9, true, latch);
                }
                else if (jobId == ALERTING_JOB_3)
                {
                    CountDownLatch latch2 = new CountDownLatch(2);
                    // do 2 jobs in parallel
                    dl = new DataUploader(new EngineApiClient(baseUrl), ALERTING_JOB_3_B, networkDataFile, latch2);
                    dl.initiateUpload(); // makes the job active
                    uploaderThreads.add(new Thread(dl));

                    longPoll = new LongPollAlertTest(client, ALERTING_JOB_3_B, null, 15.0, false, latch2);
                    longPollTests.add(longPoll);
                    Thread testThread = new Thread(longPoll, ALERTING_JOB_3_B);
                    alertTestThreads.add(testThread);
                    testThread.start();


                    longPoll = new LongPollAlertTest(client, jobId, null, 5.0, false, latch);
                }
                else if (jobId == ALERTING_JOB_2)
                {
                    longPoll = new LongPollAlertTest(client, jobId, 7.0, null, false, latch);
                }
                else
                {
                    // have 2 alerters for this job
                    longPoll = new LongPollAlertTest(client, jobId, 14.0, 2.3, false, latch);
                    Thread th = new Thread(longPoll);
                    alertTestThreads.add(th);
                    th.start();

                    longPoll = new LongPollAlertTest(client, jobId, 4.5, 2.3, false, latch);
                    longPollTests.add(longPoll);
                }

                longPollTests.add(longPoll);
                Thread testThread = new Thread(longPoll, jobId);
                alertTestThreads.add(testThread);
                testThread.start();

                for (Thread th : uploaderThreads)
                {
                    th.start();
                }

                for (Thread th : uploaderThreads)
                {
                    th.join();
                }

                // if alerting threads haven't stopped now they never will
                for (LongPollAlertTest test : longPollTests)
                {
                    test.quit();
                }

                for (Thread th : alertTestThreads)
                {
                    th.join();
                }

                for (LongPollAlertTest test : longPollTests)
                {
                    passed = passed && test.isTestPassed();
                }
            }

            // HACK - wait for the records to be written to Elasticsearch
            Thread.sleep(2000);

            for (URI uri : this.m_BucketUris)
            {
                boolean uriOk = validateBucketUrl(client, uri, 0.0);
                passed =  passed && uriOk;
            }

            if (passed)
            {
                LOGGER.info("All alert tests passed");

                for (String s : JOB_IDS)
                {
                    client.deleteJob(s);
                }
            }
            else
            {
                LOGGER.info("Alert tests failed");
                System.exit(1);
            }
        }
    }


    public static void main(String[] args)
    throws FileNotFoundException, IOException, InterruptedException
    {
        configureLogging();

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

        new AlertSubscriptionTest().runTests(baseUrl, prelertTestDataHome);
    }
}
