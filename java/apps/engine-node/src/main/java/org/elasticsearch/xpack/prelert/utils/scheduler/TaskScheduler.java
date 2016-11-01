/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.prelert.utils.scheduler;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.util.concurrent.EsExecutors;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * A scheduler that allows the periodic execution of a task
 * in a separate thread
 */
public class TaskScheduler {

    private static final Logger LOGGER = Loggers.getLogger(TaskScheduler.class);

    private final ScheduledExecutorService executor;
    private final Runnable task;
    private final Supplier<LocalDateTime> nextTimeSupplier;

    // NORELEASE Task scheduler should be using a thread pool
    public TaskScheduler(Runnable task, Supplier<LocalDateTime> nextTimeSupplier) {
        executor = Executors.newScheduledThreadPool(1, EsExecutors.daemonThreadFactory("task_scheduler"));
        this.task = Objects.requireNonNull(task);
        this.nextTimeSupplier = Objects.requireNonNull(nextTimeSupplier);
    }

    public void start() {
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
    public boolean stop(long timeout, TimeUnit unit) {
        executor.shutdownNow();
        try {
            boolean success = executor.awaitTermination(timeout, unit);
            if (!success) {
                LOGGER.warn("Waiting for running task to terminate timed out");
            }
            return success;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void scheduleNext() {
        long delay = computeNextDelay();
        executor.schedule(runTaskAndReschedule(), delay, TimeUnit.MILLISECONDS);
    }

    private Runnable runTaskAndReschedule() {
        return () -> {
            task.run();
            scheduleNext();
        };
    }

    private long computeNextDelay() {
        return LocalDateTime.now(ZoneId.systemDefault()).until(nextTimeSupplier.get(), ChronoUnit.MILLIS);
    }

    /**
     * Create a scheduler to run a task every day at midnight (local timezone) plus the
     * given offset in minutes
     * @param task the task to be scheduled
     * @param offsetMinutes the offset in minutes
     * @return the scheduler
     */
    public static TaskScheduler newMidnightTaskScheduler(Runnable task, long offsetMinutes) {
        return new TaskScheduler(task, () -> LocalDate.now(ZoneId.systemDefault()).plusDays(1).atStartOfDay().plusMinutes(offsetMinutes));
    }
}
