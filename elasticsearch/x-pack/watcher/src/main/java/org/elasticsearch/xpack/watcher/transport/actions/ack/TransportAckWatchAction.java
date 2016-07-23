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

package org.elasticsearch.xpack.watcher.transport.actions.ack;

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
import org.elasticsearch.xpack.watcher.WatcherService;
import org.elasticsearch.xpack.watcher.transport.actions.WatcherTransportAction;
import org.elasticsearch.xpack.watcher.watch.WatchStatus;
import org.elasticsearch.xpack.watcher.watch.WatchStore;

/**
 * Performs the ack operation.
 */
public class TransportAckWatchAction extends WatcherTransportAction<AckWatchRequest, AckWatchResponse> {

    private final WatcherService watcherService;

    @Inject
    public TransportAckWatchAction(Settings settings, TransportService transportService, ClusterService clusterService,
                                   ThreadPool  threadPool, ActionFilters actionFilters,
                                   IndexNameExpressionResolver indexNameExpressionResolver, WatcherService watcherService,
                                   XPackLicenseState licenseState) {
        super(settings, AckWatchAction.NAME, transportService, clusterService, threadPool, actionFilters, indexNameExpressionResolver,
                licenseState, AckWatchRequest::new);
        this.watcherService = watcherService;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.MANAGEMENT;
    }

    @Override
    protected AckWatchResponse newResponse() {
        return new AckWatchResponse();
    }

    @Override
    protected void masterOperation(AckWatchRequest request, ClusterState state, ActionListener<AckWatchResponse> listener) throws
            ElasticsearchException {
        try {
            WatchStatus watchStatus = watcherService.ackWatch(request.getWatchId(), request.getActionIds(), request.masterNodeTimeout());
            AckWatchResponse response = new AckWatchResponse(watchStatus);
            listener.onResponse(response);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }

    @Override
    protected ClusterBlockException checkBlock(AckWatchRequest request, ClusterState state) {
        return state.blocks().indexBlockedException(ClusterBlockLevel.WRITE, WatchStore.INDEX);
    }


}
