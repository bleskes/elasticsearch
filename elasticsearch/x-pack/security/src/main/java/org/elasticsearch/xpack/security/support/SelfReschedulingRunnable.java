/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */

package org.elasticsearch.xpack.security.support;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.concurrent.AbstractRunnable;
import org.elasticsearch.common.util.concurrent.FutureUtils;
import org.elasticsearch.threadpool.ThreadPool;

import java.util.concurrent.ScheduledFuture;

public class SelfReschedulingRunnable extends AbstractRunnable {

    private final AbstractRunnable runnable;
    private final ThreadPool threadPool;
    private final TimeValue interval;
    private final String executorName;
    private final ESLogger logger;

    private ScheduledFuture<?> scheduledFuture = null;
    private volatile boolean run = false;

    public SelfReschedulingRunnable(AbstractRunnable runnable, ThreadPool threadPool, TimeValue interval, String executorName,
                                    ESLogger logger) {
        this.runnable = runnable;
        this.threadPool = threadPool;
        this.interval = interval;
        this.executorName = executorName;
        this.logger = logger;
    }

    public synchronized void start() {
        if (run != false || scheduledFuture != null) {
            throw new IllegalStateException("start should not be called again before calling stop");
        }
        run = true;
        scheduledFuture = threadPool.schedule(interval, executorName, this);
    }

    @Override
    public synchronized void onAfter() {
        if (run) {
            scheduledFuture = threadPool.schedule(interval, executorName, this);
        }
    }

    @Override
    public void onFailure(Exception e) {
        logger.warn("failed to run scheduled task", e);
    }

    @Override
    protected void doRun() throws Exception {
        if (run) {
            runnable.run();
        }
    }

    public synchronized void stop() {
        if (run == false) {
            throw new IllegalStateException("stop called but not started or stop called twice");
        }
        run = false;
        FutureUtils.cancel(scheduledFuture);
        scheduledFuture = null;
    }

    // package private for testing
    ScheduledFuture<?> getScheduledFuture() {
        return scheduledFuture;
    }
}
