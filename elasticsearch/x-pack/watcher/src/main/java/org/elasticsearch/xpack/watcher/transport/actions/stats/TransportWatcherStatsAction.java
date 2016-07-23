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

package org.elasticsearch.xpack.watcher.transport.actions.stats;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.plugin.core.XPackLicenseState;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.watcher.WatcherBuild;
import org.elasticsearch.xpack.watcher.WatcherLifeCycleService;
import org.elasticsearch.xpack.watcher.WatcherService;
import org.elasticsearch.xpack.watcher.execution.ExecutionService;
import org.elasticsearch.xpack.watcher.transport.actions.WatcherTransportAction;

/**
 * Performs the stats operation.
 */
public class TransportWatcherStatsAction extends WatcherTransportAction<WatcherStatsRequest, WatcherStatsResponse> {

    private final WatcherService watcherService;
    private final ExecutionService executionService;
    private final WatcherLifeCycleService lifeCycleService;

    @Inject
    public TransportWatcherStatsAction(Settings settings, TransportService transportService, ClusterService clusterService,
                                       ThreadPool threadPool, ActionFilters actionFilters,
                                       IndexNameExpressionResolver indexNameExpressionResolver, WatcherService watcherService,
                                       ExecutionService executionService, XPackLicenseState licenseState,
                                       WatcherLifeCycleService lifeCycleService) {
        super(settings, WatcherStatsAction.NAME, transportService, clusterService, threadPool, actionFilters, indexNameExpressionResolver,
                licenseState, WatcherStatsRequest::new);
        this.watcherService = watcherService;
        this.executionService = executionService;
        this.lifeCycleService = lifeCycleService;
    }

    @Override
    protected String executor() {
        // cheap operation, no need to fork into another thread
        return ThreadPool.Names.SAME;
    }

    @Override
    protected WatcherStatsResponse newResponse() {
        return new WatcherStatsResponse();
    }

    @Override
    protected void masterOperation(WatcherStatsRequest request, ClusterState state, ActionListener<WatcherStatsResponse> listener) throws
            ElasticsearchException {
        WatcherStatsResponse statsResponse = new WatcherStatsResponse();
        statsResponse.setWatcherState(watcherService.state());
        statsResponse.setThreadPoolQueueSize(executionService.executionThreadPoolQueueSize());
        statsResponse.setWatchesCount(watcherService.watchesCount());
        statsResponse.setThreadPoolMaxSize(executionService.executionThreadPoolMaxSize());
        statsResponse.setBuild(WatcherBuild.CURRENT);
        statsResponse.setWatcherMetaData(lifeCycleService.watcherMetaData());

        if (request.includeCurrentWatches()) {
            statsResponse.setSnapshots(executionService.currentExecutions());
        }
        if (request.includeQueuedWatches()) {
            statsResponse.setQueuedWatches(executionService.queuedWatches());
        }

        listener.onResponse(statsResponse);
    }

    @Override
    protected ClusterBlockException checkBlock(WatcherStatsRequest request, ClusterState state) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_READ);
    }


}
