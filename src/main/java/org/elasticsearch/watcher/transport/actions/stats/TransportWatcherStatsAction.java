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

package org.elasticsearch.watcher.transport.actions.stats;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.watcher.WatcherBuild;
import org.elasticsearch.watcher.WatcherVersion;
import org.elasticsearch.watcher.execution.ExecutionService;
import org.elasticsearch.watcher.license.LicenseService;
import org.elasticsearch.watcher.transport.actions.WatcherTransportAction;
import org.elasticsearch.watcher.watch.WatchService;

/**
 * Performs the stats operation.
 */
public class TransportWatcherStatsAction extends WatcherTransportAction<WatcherStatsRequest, WatcherStatsResponse> {

    private final WatchService watchService;
    private final ExecutionService executionService;

    @Inject
    public TransportWatcherStatsAction(Settings settings, TransportService transportService, ClusterService clusterService,
                                       ThreadPool threadPool, ActionFilters actionFilters, WatchService watchService,
                                       ExecutionService executionService, LicenseService licenseService) {
        super(settings, WatcherStatsAction.NAME, transportService, clusterService, threadPool, actionFilters, licenseService);
        this.watchService = watchService;
        this.executionService = executionService;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.MANAGEMENT;
    }

    @Override
    protected WatcherStatsRequest newRequest() {
        return new WatcherStatsRequest();
    }

    @Override
    protected WatcherStatsResponse newResponse() {
        return new WatcherStatsResponse();
    }

    @Override
    protected void masterOperation(WatcherStatsRequest request, ClusterState state, ActionListener<WatcherStatsResponse> listener) throws ElasticsearchException {
        WatcherStatsResponse statsResponse = new WatcherStatsResponse();
        statsResponse.setWatchServiceState(watchService.state());
        statsResponse.setWatchExecutionQueueSize(executionService.queueSize());
        statsResponse.setWatchesCount(watchService.watchesCount());
        statsResponse.setWatchExecutionQueueMaxSize(executionService.largestQueueSize());
        statsResponse.setVersion(WatcherVersion.CURRENT);
        statsResponse.setBuild(WatcherBuild.CURRENT);
        listener.onResponse(statsResponse);
    }

    @Override
    protected ClusterBlockException checkBlock(WatcherStatsRequest request, ClusterState state) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA);
    }


}
