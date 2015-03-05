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

package org.elasticsearch.alerts.history;

import org.elasticsearch.alerts.AlertsPlugin;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.util.concurrent.EsThreadPoolExecutor;
import org.elasticsearch.threadpool.ThreadPool;

import java.util.concurrent.BlockingQueue;

/**
 *
 */
public class InternalAlertsExecutor implements AlertsExecutor {

    private final ThreadPool threadPool;

    @Inject
    public InternalAlertsExecutor(ThreadPool threadPool) {
        this.threadPool = threadPool;
    }

    @Override
    public BlockingQueue queue() {
        return executor().getQueue();
    }

    @Override
    public long largestPoolSize() {
        return executor().getLargestPoolSize();
    }

    @Override
    public void execute(Runnable runnable) {
        executor().execute(runnable);
    }

    private EsThreadPoolExecutor executor() {
        return (EsThreadPoolExecutor) threadPool.executor(AlertsPlugin.NAME);
    }
}