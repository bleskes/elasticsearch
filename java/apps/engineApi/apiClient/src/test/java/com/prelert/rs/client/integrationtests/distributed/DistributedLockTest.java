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

package com.prelert.rs.client.integrationtests.distributed;

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
import com.prelert.rs.client.integrationtests.BaseIntegrationTest;
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
 * is processing data for the job. This is all done concurrently in multiple threads<br>
 *
 * Next the job is closed and moved to another host and the same tests are
 * run again.
 */
public class DistributedLockTest extends BaseIntegrationTest
{
    private String [] m_EngineApiUrls;

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
        String jobId = dataUploader.getJobId();

        String [] otherHostUrls = new String [m_EngineApiUrls.length -1];
        System.arraycopy(m_EngineApiUrls, 1, otherHostUrls, 0, m_EngineApiUrls.length -1);

        try
        {
            String expectedHostname = hostnameFromUrl(m_EngineApiUrls[0]);
            doConcurrentActionOnDifferentNodeTest(jobId, otherHostUrls, expectedHostname);
        }
        catch (InterruptedException | ExecutionException e1)
        {
            throw new IllegalStateException(e1);
        }
        finally
        {
            stopDataUploaderAndJoinThread(dataUploader);
        }

        // job should be sleeping now
        testSleepingProcessOnOneNodeBlocksActionsOnOtherNodes(jobId, otherHostUrls);

        m_EngineApiClient.closeJob(jobId);

        // restart job on another node and run the same tests
        String jobHostUrl = m_EngineApiUrls[1];
        otherHostUrls[0] = m_EngineApiUrls[0];
        if (m_EngineApiUrls.length > 2)
        {
            System.arraycopy(m_EngineApiUrls, 2, otherHostUrls, 2, m_EngineApiUrls.length -2);
        }
        dataUploader = startDataUploader(jobHostUrl, jobId);

        try
        {
            String expectedHostname = hostnameFromUrl(jobHostUrl);
            doConcurrentActionOnDifferentNodeTest(jobId, otherHostUrls, expectedHostname);
        }
        catch (InterruptedException | ExecutionException e1)
        {
            throw new IllegalStateException(e1);
        }
        finally
        {
            stopDataUploaderAndJoinThread(dataUploader);
        }

        m_EngineApiClient.deleteJob(jobId);
    }

    private void doConcurrentActionOnDifferentNodeTest(String jobId, String [] hostUrls,
                                            String expectedJobhost)
            throws InterruptedException, ExecutionException
    {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(hostUrls.length -1);
        try
        {
            // Need to get the futures to execute and call get() on it
            // so exceptions from the executor are propagated
            List<ScheduledFuture<?>> futures = new ArrayList<ScheduledFuture<?>>();

            // test operations not allowed on other nodes
            for (int i=0; i<hostUrls.length; i++)
            {
                ScheduledFuture<?> future = executor.schedule(
                        new ConcurrentActionClient(hostUrls[i], jobId,
                                            Optional.of(m_CountDownLatch), expectedJobhost),
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
                // test writing
                String data = CsvDataRunner.HEADER + "\n1000,metric,100\n";
                InputStream is = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
                MultiDataPostResult result = client.streamingUpload(jobId, is, false);


                test(result.anErrorOccurred(), "Writing data: An error should have occurred");
                test(result.getResponses().size() == 1, "Writing data: Should have a single response");

                ApiError apiError = result.getResponses().get(0).getError();
                test(apiError.getErrorCode() == ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR,
                                        "Writing data: Error code should be job in use error");

                // test updating
                String updateJson = "{\"modelDebugConfig\":{\"boundsPercentile\":90.0, \"terms\":\"someTerm\"}}";
                test(client.updateJob(jobId, updateJson) == false);
                apiError = client.getLastError();
                test(apiError != null);
                test(apiError.getErrorCode() == ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR,
                        "Updating job: Error code should be job in use error");


                // and test resuming
                boolean resumed = client.resumeJob(jobId);
                if (resumed)
                {
                    throw new IllegalStateException("Error: resumed job while it was sleeping on another node");
                }

                apiError = client.getLastError();

                if (apiError.getErrorCode() != ErrorCodes.CANNOT_RESUME_JOB)
                {
                    throw new IllegalStateException("Resuming Job: Error code should be job in use error");
                }

            }
        }
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
