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

package com.prelert.utils.scheduler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.log4j.Logger;

/**
 * A scheduler that allows the periodic execution of a task
 * in a separate thread
 */
public class TaskScheduler
{
    private static final Logger LOGGER = Logger.getLogger(TaskScheduler.class);

    private final ScheduledExecutorService m_Executor;
    private final Runnable m_Task;
    private final Supplier<LocalDateTime> m_NextTimeSupplier;

    public TaskScheduler(Runnable task, Supplier<LocalDateTime> nextTimeSupplier)
    {
        m_Executor = Executors.newScheduledThreadPool(1);
        m_Task = Objects.requireNonNull(task);
        m_NextTimeSupplier = Objects.requireNonNull(nextTimeSupplier);
    }

    public void start()
    {
        scheduleNext();
    }

    /**
     * Attempts to stop the running task, if any, and cancels all future tasks.
     * The method blocks until the scheduler has stopped, or if it times out.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return {@code true} if the scheduler stopped before timing out, or {@code false} otherwise
     */
    public boolean stop(long timeout, TimeUnit unit)
    {
        m_Executor.shutdownNow();
        try
        {
            boolean success = m_Executor.awaitTermination(timeout, unit);
            if (!success)
            {
                LOGGER.warn("Waiting for running task to terminate timed out");
            }
            return success;
        } catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void scheduleNext()
    {
        long delay = computeNextDelay();
        m_Executor.schedule(runTaskAndReschedule(), delay, TimeUnit.MILLISECONDS);
    }

    private Runnable runTaskAndReschedule()
    {
        return () -> {
            m_Task.run();
            scheduleNext();
        };
    }

    private long computeNextDelay()
    {
        return LocalDateTime.now().until(m_NextTimeSupplier.get(), ChronoUnit.MILLIS);
    }

    /**
     * Create a scheduler to run a task every day at midnight (local timezone) plus the
     * given offset in minutes
     * @param task the task to be scheduled
     * @param offsetMinutes the offset in minutes
     * @return the scheduler
     */
    public static TaskScheduler newMidnightTaskScheduler(Runnable task, long offsetMinutes)
    {
        return new TaskScheduler(task, () -> LocalDate.now().plusDays(1).atStartOfDay()
                .plusMinutes(offsetMinutes));
    }
}
