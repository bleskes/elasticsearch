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
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import org.apache.log4j.Logger;

import com.prelert.app.Shutdownable;
import com.prelert.job.UnknownJobException;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.process.exceptions.NativeProcessRunException;

/**
 * Manages the automatic closing of running jobs.
 * When a job is started to run it is expected to be added to the {@code JobAutoCloser}.
 * Jobs will automatically be closed either after their timeout expires or upon shutdown.
 * Jobs with zero or negative timeout will only be closed upon shutdown.
 *
 * An optional can close function can be passed to the constructor in which
 * case when a job times out it is only closed if can close returns true.
 * If can close returns false the timeout is rescheduled. This is to catch the
 * situation where the datastore is down so the job model state wouldn't be
 * persisted if the job tries to close.
 */
class JobAutoCloser implements Shutdownable
{
    @FunctionalInterface
    interface JobCloser
    {
        void closeJob(String jobId) throws UnknownJobException, JobInUseException,
                NativeProcessRunException;
    }

    private static final Logger LOGGER = Logger.getLogger(JobAutoCloser.class);
    private static final int WAIT_SECONDS_BEFORE_RETRY_CLOSING = 10;
    private static final int MILLIS_IN_SECOND = 1000;
    private static final long CANNOT_CLOSE_RETRY_TIME = 300;
    private static final NullScheduledFuture NULL_FUTURE = new NullScheduledFuture();

    private final JobCloser m_JobCloser;
    private final ScheduledExecutorService m_ScheduledExecutor;
    private final ConcurrentMap<String, ScheduledFuture<?>> m_JobIdToTimeoutFuture;

    private final Function<String, Boolean> m_CanClose;

    private final long m_WaitBeforeRetryMillis;

    public JobAutoCloser(JobCloser jobCloser)
    {
        this(jobCloser, (s) -> true, WAIT_SECONDS_BEFORE_RETRY_CLOSING * MILLIS_IN_SECOND);
    }

    /**
     *
     * @param jobCloser Closes the job
     * @param okToClose Takes the job Id as parameter and returns true if
     * it's Ok to close the job.
     */
    public JobAutoCloser(JobCloser jobCloser, Function<String, Boolean> okToClose)
    {
        this(jobCloser, okToClose, WAIT_SECONDS_BEFORE_RETRY_CLOSING * MILLIS_IN_SECOND);
    }

    JobAutoCloser(JobCloser jobCloser, Function<String, Boolean> okToClose, long waitBeforeRetryMillis)
    {
        m_JobCloser = Objects.requireNonNull(jobCloser);
        m_CanClose = Objects.requireNonNull(okToClose);
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
     * @param timeout The duration of the timeout. If <= 0 no timeout will be applied.
     * @return
     */
    public void startTimeout(String jobId, Duration timeout)
    {
        if (timeout.isZero() || timeout.isNegative())
        {
            m_JobIdToTimeoutFuture.put(jobId, NULL_FUTURE);
        }
        else
        {
            ScheduledFuture<?> scheduledFuture = m_ScheduledExecutor.schedule(
                    new FinishJobRunnable(jobId), timeout.toMillis(), TimeUnit.MILLISECONDS);
            m_JobIdToTimeoutFuture.put(jobId, scheduledFuture);
        }
    }

    /**
     * Cancels the timeout for the given job.
     * @param jobId
     */
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
            LOGGER.trace("No future to cancel for job:" + jobId);
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
            if (m_CanClose.apply(m_JobId))
            {
                LOGGER.info("Timeout expired stopping process for job:" + m_JobId);
                tryClosingJob(m_JobId);
            }
            else
            {
                LOGGER.info("Timeout expired, denied request to close job: " + m_JobId +
                        " Retry in " + CANNOT_CLOSE_RETRY_TIME + " seconds");
                startTimeout(m_JobId, Duration.of(CANNOT_CLOSE_RETRY_TIME, ChronoUnit.SECONDS));
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
     * Stop the job auto closer by shutting down the executor
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

        LOGGER.info(String.format("Closing %d running jobs", m_JobIdToTimeoutFuture.size()));

        for (String jobId : m_JobIdToTimeoutFuture.keySet())
        {
            tryClosingJob(jobId);
        }
    }

    private void tryClosingJob(String jobId)
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
                LOGGER.error("Error closing job " + jobId, e);
                return;
            }
        }
    }

    private static class NullScheduledFuture implements ScheduledFuture<Void>
    {
        @Override
        public long getDelay(TimeUnit unit)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public int compareTo(Delayed o)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning)
        {
            return true;
        }

        @Override
        public boolean isCancelled()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isDone()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Void get() throws InterruptedException, ExecutionException
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Void get(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException
        {
            throw new UnsupportedOperationException();
        }
    }
}
