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

package org.elasticsearch.watcher.execution;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.common.util.concurrent.EsThreadPoolExecutor;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.watcher.WatcherPlugin;
import org.elasticsearch.watcher.support.ThreadPoolSettingsBuilder;

import java.util.concurrent.BlockingQueue;

/**
 *
 */
public class InternalWatchExecutor implements WatchExecutor {

    public static final String THREAD_POOL_NAME = WatcherPlugin.NAME;

    public static Settings additionalSettings(Settings nodeSettings) {
        Settings settings = nodeSettings.getAsSettings("threadpool." + THREAD_POOL_NAME);
        if (!settings.names().isEmpty()) {
            // the TP is already configured in the node settings
            // no need for additional settings
            return ImmutableSettings.EMPTY;
        }
        int availableProcessors = EsExecutors.boundedNumberOfProcessors(nodeSettings);
        return new ThreadPoolSettingsBuilder.Fixed(THREAD_POOL_NAME)
                .size(5 * availableProcessors)
                .queueSize(1000)
                .build();
    }

    private final ThreadPool threadPool;

    @Inject
    public InternalWatchExecutor(ThreadPool threadPool) {
        this.threadPool = threadPool;
    }

    @Override
    public BlockingQueue<Runnable> queue() {
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
        return (EsThreadPoolExecutor) threadPool.executor(THREAD_POOL_NAME);
    }
}