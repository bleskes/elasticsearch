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

package com.prelert.rs.client.datauploader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Logger;

import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.rs.client.EngineApiClient;
import com.prelert.rs.data.ApiError;
import com.prelert.rs.data.MultiDataPostResult;

/**
 * The run() method tries to perform actions that are not allowed
 * to happen concurrently while the job is processing data.
 * Actions not allowed to happen concurrently are
 * <ol>
 * <li>Close</li>
 * <li>Flush</li>
 * <li>Write</li>
 * <li>Delete</li>
 * <li>Pause</li>
 * <li>Resume</li>
 * </ol>
 *
 *
 * Throws an {@link IllegalStateException} if it succeeds in performing
 * one of the actions which should be disallowed
 *
 */
public class ConcurrentActionClient implements Runnable
{
    private static final Logger LOGGER = Logger.getLogger(ConcurrentActionClient.class);
    private String m_Url;
    private String m_JobId;
    private Optional<CountDownLatch> m_CountDownLatch;
    private String m_ExpectedHostname;


    /**
     *
     * @param url Host url to test
     * @param jobId This job should be processing data elsewhere
     * @param latch Optional latch for synchronisation. run() counts down the
     *  latch when complete.
     */
    public ConcurrentActionClient(String url, String jobId, Optional<CountDownLatch> latch)
    {
        this(url, jobId, latch, null);
    }

    /**
     *
     * @param url Host url to test
     * @param jobId This job should be processing data elsewhere
     * @param latch Optional latch for synchronisation. run() counts down the
     * @param expectedHostname Check the {@linkplain ApiError}s have this
     * value in its host field.
     */
    public ConcurrentActionClient(String url, String jobId, Optional<CountDownLatch> latch,
                                String expectedHostname)
    {
        m_Url = url;
        m_JobId = jobId;
        m_CountDownLatch = latch;
        m_ExpectedHostname = expectedHostname;
    }

    @Override
    public void run()
    {
        try (EngineApiClient client = new EngineApiClient(m_Url))
        {
            // 1. Cannot close a job when another process is writing to it
            boolean closed = client.closeJob(m_JobId);
            if (closed)
            {
                throw new IllegalStateException("Error: closed job while writing to it");
            }

            ApiError apiError = client.getLastError();

            if (apiError.getErrorCode() != ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR)
            {
                throw new IllegalStateException("Closing Job: Error code should be job in use error");
            }
            checkExpectedHost(apiError);

            // 2. Cannot flush a job when another process is writing to it
            boolean flushed = client.flushJob(m_JobId, false);
            if (flushed)
            {
                throw new IllegalStateException("Error: flushed job while writing to it");
            }

            apiError = client.getLastError();
            if (apiError.getErrorCode() != ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR)
            {
                throw new IllegalStateException("Flushing Job: Error code should be job in use error");
            }
            checkExpectedHost(apiError);


            // 3. Cannot write to the job when another process is writing to it
            String data = CsvDataRunner.HEADER + "\n1000,metric,100\n";
            InputStream is = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
            MultiDataPostResult result = client.streamingUpload(m_JobId, is, false);

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
            checkExpectedHost(apiError);

            if (result.getResponses().get(0).getUploadSummary() != null)
            {
                throw new IllegalStateException("Error: wrote to job in use");
            }

            // 4. Cannot delete a job when another process is writing to it
            boolean deleted = client.deleteJob(m_JobId);
            if (deleted)
            {
                throw new IllegalStateException("Error: deleted job while writing to it");
            }

            apiError = client.getLastError();
            if (apiError.getErrorCode() != ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR)
            {
                throw new IllegalStateException("Deleting Job: Error code should be job in use error");
            }
            checkExpectedHost(apiError);

            // 5. Cannot pause a job when another process is writing to it
            boolean paused = client.pauseJob(m_JobId);
            if (paused)
            {
                throw new IllegalStateException("Error: paused job while writing to it");
            }

            apiError = client.getLastError();
            if (apiError.getErrorCode() != ErrorCodes.CANNOT_PAUSE_JOB)
            {
                throw new IllegalStateException("Pausing Job: Error code should be job in use error");
            }
            checkExpectedHost(apiError);

            // 6. Cannot resume a job when another process is writing to it
            boolean resumed = client.resumeJob(m_JobId);
            if (resumed)
            {
                throw new IllegalStateException("Error: resumed job while writing to it");
            }

            apiError = client.getLastError();
            if (apiError.getErrorCode() != ErrorCodes.CANNOT_RESUME_JOB)
            {
                throw new IllegalStateException("Resuming Job: Error code should be job in use error");
            }
//            checkExpectedHost(apiError);

        }
        catch (IOException e)
        {
            LOGGER.error("Exception in concurrent client test " + m_Url, e);
        }
        finally
        {
            if (m_CountDownLatch.isPresent())
            {
                m_CountDownLatch.get().countDown();
            }
        }
    }

    private void checkExpectedHost(ApiError error)
    {
        if (m_ExpectedHostname != null)
        {
            if (!m_ExpectedHostname.equals(error.getHostname()))
            {
                throw new IllegalStateException(
                        String.format("Expected host %s got %s", m_ExpectedHostname, error.getHostname()));
            }
        }
    }
}
