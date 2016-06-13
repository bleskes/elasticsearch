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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.rs.client.EngineApiClient;
import com.prelert.rs.client.datauploader.ConcurrentActionClient;
import com.prelert.rs.client.datauploader.CsvDataRunner;
import com.prelert.rs.data.ApiError;
import com.prelert.rs.data.MultiDataPostResult;


/**
 * Test the distributed lock.
 * main takes a list of URLs to the engine API nodes e.g.
 *  http://localhost:8080/engine/v2 http://marple:8080/engineApi/v2 http://aws:8080/engineApi/v2
 * There must be at least 2 URLs provided
 *
 * The test creates a job on the first node and starts uploading data to it.
 * For each of the other nodes some operation such as delete is tried on the
 * first job, the test asserts that the operation fails while the first node
 * is processing data for the job.
 * This is all done concurrently in multiple threads
 *
 */
public class DistributedLockTest extends BaseIntegrationTest
{
    private String [] m_EngineApiUrls;

    private Thread m_DataUploaderThread;

    private CountDownLatch m_CountDownLatch;

    public DistributedLockTest(String [] engineApiUrls)
    {
        super(engineApiUrls[0]);

        m_CountDownLatch = new CountDownLatch(engineApiUrls.length -1);
        m_EngineApiUrls = engineApiUrls;
    }

    @Override
    protected void runTest() throws IOException
    {
        // create job on the first node and start uploading data
        CsvDataRunner dataUploader = startDataUploader(m_EngineApiUrls[0]);

        String [] otherHostUrls = new String [m_EngineApiUrls.length -1];
        System.arraycopy(m_EngineApiUrls, 1, otherHostUrls, 0, m_EngineApiUrls.length -1);

        try
        {
            doConcurrentActionOnDifferentNodeTest(dataUploader.getJobId(), otherHostUrls);
        }
        catch (InterruptedException | ExecutionException e1)
        {
            throw new IllegalStateException(e1);
        }
        finally
        {
            // stop uploader and join threads - this doesn't close the job
            dataUploader.cancel();
            try
            {
                m_Logger.info("Waiting on upload thread to finish");
                m_DataUploaderThread.join();
            }
            catch (InterruptedException e)
            {
                m_Logger.error("Interupted joining test thread", e);
            }
        }

        // job should be sleeping now
        testSleepingProcessOnOneNodeBlocksActionsOnOtherNodes(dataUploader.getJobId(), otherHostUrls);

        m_EngineApiClient.deleteJob(dataUploader.getJobId());
    }

    private void doConcurrentActionOnDifferentNodeTest(String jobId, String [] hostUrls)
            throws InterruptedException, ExecutionException
    {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(m_EngineApiUrls.length -1);
        try
        {
            // Need to get the futures to execute and call get() on it
            // so exceptions from the executor are propagated
            List<ScheduledFuture<?>> futures = new ArrayList<ScheduledFuture<?>>();

            // test operations not allowed on other nodes
            for (int i=0; i<hostUrls.length; i++)
            {
                ScheduledFuture<?> future = executor.schedule(
                        new ConcurrentActionClient(hostUrls[i], jobId, Optional.of(m_CountDownLatch)),
                                0, TimeUnit.SECONDS);

                futures.add(future);
            }

            // wait for test clients to finish
            try
            {
                m_CountDownLatch.await();
            }
            catch (InterruptedException e1)
            {
                m_Logger.error(e1);
            }

            // get() throws if there was an exception
            for (ScheduledFuture<?> f : futures)
            {
                f.get();
            }
        }
        finally
        {
            executor.shutdown();
        }
    }

    private void testSleepingProcessOnOneNodeBlocksActionsOnOtherNodes(String jobId, String [] hostUrls)
            throws IOException
    {
        for (String url : hostUrls)
        {
            try (EngineApiClient client = new EngineApiClient(url))
            {
                String data = CsvDataRunner.HEADER + "\n1000,metric,100\n";
                InputStream is = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
                MultiDataPostResult result = client.streamingUpload(jobId, is, false);


                test(result.anErrorOccurred(), "Writing data: An error should have occurred");
                test(result.getResponses().size() == 1, "Writing data: Should have a single response");

                ApiError apiError = result.getResponses().get(0).getError();
                test(apiError.getErrorCode() == ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR,
                                        "Writing data: Error code should be job in use error");

                String updateJson = "{\"modelDebugConfig\":{\"boundsPercentile\":90.0, \"terms\":\"someTerm\"}}";
                test(client.updateJob(jobId, updateJson) == false);
                apiError = client.getLastError();
                test(apiError != null);
                test(apiError.getErrorCode() == ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR,
                        "Updating job: Error code should be job in use error");
            }
        }
    }

    /**
     * Does not return the data runner until it has started uploading
     *
     * @param url
     * @return
     * @throws IOException
     */
    private CsvDataRunner startDataUploader(String url) throws IOException
    {
        CsvDataRunner jobRunner = new CsvDataRunner(url);
        jobRunner.createJob();

        m_DataUploaderThread = new Thread(jobRunner);
        m_DataUploaderThread.start();


        // wait for the runner thread to start the upload
        synchronized (jobRunner)
        {
            try
            {
                jobRunner.wait();
            }
            catch (InterruptedException e1)
            {
                m_Logger.error(e1);
            }
        }

        return jobRunner;
    }

    public static void main(String[] args) throws IOException
    {
        // expect at least 2 hosts
        if (args.length <= 1)
        {
            // Logging not configured yet
            String msg = "At least 2 host URLS should be provided as arguments to main";
            System.out.println(msg);
            throw new IllegalArgumentException(msg);
        }


        try (DistributedLockTest test = new DistributedLockTest(args))
        {
            test.m_Logger.info("Starting Distributed Lock test");
            test.runTest();
            test.m_Logger.info("Distributed Lock test passed");
        }
    }
}
