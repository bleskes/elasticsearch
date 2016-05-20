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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.rs.client.EngineApiClient;
import com.prelert.rs.client.datauploader.CsvDataRunner;
import com.prelert.rs.data.ApiError;
import com.prelert.rs.data.MultiDataPostResult;


/**
 * Test the distributed lock.
 * main takes a list of URLs to the engine API nodes e.g.
 *  http://localhost:8080 http://marple:8080/engineApi/v2 http://aws:8080/engineApi/v2
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

        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(m_EngineApiUrls.length -1);
        try
        {
            // test operations not allowed on other nodes
            for (int i=1; i<m_EngineApiUrls.length; i++)
            {
                executor.execute(new ConcurrentClientTest(m_EngineApiUrls[i], dataUploader.getJobId()));
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

            // stop uploader and join threads
            dataUploader.cancel();
            try
            {
                m_DataUploaderThread.join();
            }
            catch (InterruptedException e)
            {
                m_Logger.error("Interupted joining test thread", e);
            }
        }
        finally
        {
            executor.shutdown();
        }
    }

    private class ConcurrentClientTest implements Runnable
    {
        private String url;
        private String jobId;

        private ConcurrentClientTest(String url, String jobId)
        {
            this.url = url;
            this.jobId = jobId;
        }

        @Override
        public void run()
        {
            try (EngineApiClient client = new EngineApiClient(url))
            {
                // 1. Cannot close a job when another process is writing to it
                boolean closed = client.closeJob(jobId);
                if (closed)
                {
                    throw new IllegalStateException("Error closed job while writing to it");
                }

                ApiError apiError = client.getLastError();

                if (apiError.getErrorCode() != ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR)
                {
                    throw new IllegalStateException("Closing Job: Error code should be job in use error");
                }


                // 2. Cannot flush a job when another process is writing to it
                boolean flushed = client.flushJob(jobId, false);
                if (flushed)
                {
                    throw new IllegalStateException("Error flushed job while writing to it");
                }

                apiError = client.getLastError();

                if (apiError.getErrorCode() != ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR)
                {
                    throw new IllegalStateException("Flushing Job: Error code should be job in use error");
                }


                // 3. Cannot write to the job when another process is writing to it
                String data = CsvDataRunner.HEADER + "\n1000,metric,100\n";
                InputStream is = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
                MultiDataPostResult result = client.streamingUpload(jobId, is, false);

                if (!result.anErrorOccurred())
                {
                    throw new IllegalStateException("Writing data: An error should have occurred");
                }
                if (result.getResponses().size() != 1)
                {
                    throw new IllegalStateException("Writing data: Should have a single response");
                }

                apiError = result.getResponses().get(0).getError();
                if (apiError.getErrorCode() != ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR)
                {
                    throw new IllegalStateException("Writing data: Error code should be job in use error");
                }

                if (result.getResponses().get(0).getUploadSummary() != null)
                {
                    throw new IllegalStateException("Error wrote to job in use");
                }

                // 4. Cannot delete a job when another process is writing to it
                boolean deleted = client.deleteJob(jobId);
                if (deleted)
                {
                    throw new IllegalStateException("Error deleted job while writing to it");
                }

                apiError = client.getLastError();

                if (apiError.getErrorCode() != ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR)
                {
                    throw new IllegalStateException("Deleting Job: Error code should be job in use error");
                }
            }
            catch (IOException e)
            {
                DistributedLockTest.this.m_Logger.error("Exception in concurrent client test "
                        + url, e);
            }
            finally
            {
                DistributedLockTest.this.m_CountDownLatch.countDown();
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
