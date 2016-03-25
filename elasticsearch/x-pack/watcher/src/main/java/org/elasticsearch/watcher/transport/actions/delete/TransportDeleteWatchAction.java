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

package org.elasticsearch.watcher.transport.actions.delete;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.watcher.WatcherService;
import org.elasticsearch.watcher.license.WatcherLicensee;
import org.elasticsearch.watcher.transport.actions.WatcherTransportAction;
import org.elasticsearch.watcher.watch.WatchStore;

/**
 * Performs the delete operation.
 */
public class TransportDeleteWatchAction extends WatcherTransportAction<DeleteWatchRequest, DeleteWatchResponse> {

    private final WatcherService watcherService;

    @Inject
    public TransportDeleteWatchAction(Settings settings, TransportService transportService, ClusterService clusterService,
                                      ThreadPool threadPool, ActionFilters actionFilters,
                                      IndexNameExpressionResolver indexNameExpressionResolver, WatcherService watcherService,
                                      WatcherLicensee watcherLicensee) {
        super(settings, DeleteWatchAction.NAME, transportService, clusterService, threadPool, actionFilters, indexNameExpressionResolver,
                watcherLicensee, DeleteWatchRequest::new);
        this.watcherService = watcherService;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.MANAGEMENT;
    }

    @Override
    protected DeleteWatchResponse newResponse() {
        return new DeleteWatchResponse();
    }

    @Override
    protected void masterOperation(DeleteWatchRequest request, ClusterState state, ActionListener<DeleteWatchResponse> listener) throws
            ElasticsearchException {
        try {
            DeleteResponse deleteResponse = watcherService.deleteWatch(request.getId(), request.masterNodeTimeout(), request.isForce())
                    .deleteResponse();
            DeleteWatchResponse response = new DeleteWatchResponse(deleteResponse.getId(), deleteResponse.getVersion(), deleteResponse
                    .isFound());
            listener.onResponse(response);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }

    @Override
    protected ClusterBlockException checkBlock(DeleteWatchRequest request, ClusterState state) {
        return state.blocks().indexBlockedException(ClusterBlockLevel.WRITE, WatchStore.INDEX);
    }


}
