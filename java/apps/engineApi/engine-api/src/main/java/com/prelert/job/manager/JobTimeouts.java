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

package com.prelert.job.manager;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.prelert.app.Shutdownable;
import com.prelert.job.UnknownJobException;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.process.exceptions.NativeProcessRunException;

class JobTimeouts implements Shutdownable
{
    interface JobCloser
    {
        void closeJob(String jobId) throws UnknownJobException, JobInUseException,
                NativeProcessRunException;
    }

    private static final Logger LOGGER = Logger.getLogger(JobTimeouts.class);
    private static final int WAIT_SECONDS_BEFORE_RETRY_CLOSING = 10;

    private final JobCloser m_JobCloser;
    private final ScheduledExecutorService m_ScheduledExecutor;
    private final ConcurrentMap<String, ScheduledFuture<?>> m_JobIdToTimeoutFuture;
    private final long m_WaitBeforeRetryMillis;

    public JobTimeouts(JobCloser jobCloser)
    {
        this(jobCloser, WAIT_SECONDS_BEFORE_RETRY_CLOSING * 1000);
    }

    JobTimeouts(JobCloser jobCloser, long waitBeforeRetryMillis)
    {
        m_JobCloser = Objects.requireNonNull(jobCloser);
        m_ScheduledExecutor = Executors.newScheduledThreadPool(1);
        m_JobIdToTimeoutFuture = new ConcurrentHashMap<String, ScheduledFuture<?>>();
        m_WaitBeforeRetryMillis = waitBeforeRetryMillis;
    }

    /**
     * Add the timeout schedule for <code>jobId</code>.
     * On time out it tries to shutdown the job but if the job
     * is still running it schedules another task to try again in 10
     * seconds.
     *
     * @param jobId
     * @param timeoutSeconds
     * @return
     */
    public void startTimeout(String jobId, Duration timeout)
    {
        ScheduledFuture<?> scheduledFuture = m_ScheduledExecutor.schedule(
                new FinishJobRunnable(jobId), timeout.toMillis(), TimeUnit.MILLISECONDS);
        m_JobIdToTimeoutFuture.put(jobId, scheduledFuture);
    }

    public void stopTimeout(String jobId)
    {
        ScheduledFuture<?> future = m_JobIdToTimeoutFuture.remove(jobId);
        if (future != null)
        {
            if (future.cancel(false) == false)
            {
                LOGGER.warn("Failed to cancel timeout for job: " + jobId);
            }
        }
        else
        {
            LOGGER.debug("No future to cancel for job:" + jobId);
        }
    }

    private class FinishJobRunnable implements Runnable
    {
        private final String m_JobId;

        public FinishJobRunnable(String jobId)
        {
            m_JobId = jobId;
        }

        @Override
        public void run()
        {
            LOGGER.info("Timeout expired stopping process for job:" + m_JobId);

            try
            {
                boolean notFinished = true;
                while (notFinished)
                {
                    try
                    {
                        m_JobCloser.closeJob(m_JobId);
                        notFinished = false;
                    }
                    catch (JobInUseException e)
                    {
                        if (JobTimeouts.this.wait(m_JobId, e) == false)
                        {
                            return;
                        }
                    }
                }
            }
            catch (NativeProcessRunException | UnknownJobException e)
            {
                LOGGER.error(String.format("Error in job %s finish timeout", m_JobId), e);
            }
        }
    }

    private boolean wait(String jobId, JobInUseException e)
    {
        String msg = String.format(
                "Job '%s' is reading data and cannot be shutdown " +
                        "Rescheduling shutdown for %d milliseconds", jobId, m_WaitBeforeRetryMillis);
        LOGGER.warn(msg);

        // wait then try again
        try
        {
            Thread.sleep(m_WaitBeforeRetryMillis);
        }
        catch (InterruptedException e1)
        {
            Thread.currentThread().interrupt();
            LOGGER.warn("Interrupted waiting for job to stop", e);
            return false;
        }
        return true;
    }

    /**
     * Stop the process manager by shutting down the executor
     * service and stop all running processes. Processes won't quit
     * straight away once the input stream is closed but will stop
     * soon after once the data has been analysed.
     */
    @Override
    public void shutdown()
    {
        LOGGER.info("Stopping all Engine API Jobs");

        // Stop new being scheduled
        m_ScheduledExecutor.shutdownNow();

        LOGGER.info(String.format("Closing %d active jobs", m_JobIdToTimeoutFuture.size()));

        for (String jobId : m_JobIdToTimeoutFuture.keySet())
        {
            boolean notFinished = true;
            while (notFinished)
            {
                try
                {
                    m_JobCloser.closeJob(jobId);
                    notFinished = false;
                }
                catch (JobInUseException e)
                {
                    // wait then try again
                    if (wait(jobId, e) == false)
                    {
                        return;
                    }
                }
                catch (NativeProcessRunException | UnknownJobException e)
                {
                    LOGGER.error("Error stopping running job " + jobId, e);
                }
            }
        }
    }
}
