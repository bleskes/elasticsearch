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

package org.elasticsearch.xpack.watcher.transport.actions.delete;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.master.TransportMasterNodeAction;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.watcher.WatcherService;
import org.elasticsearch.xpack.watcher.watch.WatchStore;

/**
 * Performs the delete operation. This inherits directly from TransportMasterNodeAction, because deletion should always work
 * independently from the license check in WatcherTransportAction!
 */
public class TransportDeleteWatchAction extends TransportMasterNodeAction<DeleteWatchRequest, DeleteWatchResponse> {

    private final WatcherService watcherService;

    @Inject
    public TransportDeleteWatchAction(Settings settings, TransportService transportService, ClusterService clusterService,
                                      ThreadPool threadPool, ActionFilters actionFilters,
                                      IndexNameExpressionResolver indexNameExpressionResolver, WatcherService watcherService) {
        super(settings, DeleteWatchAction.NAME, transportService, clusterService, threadPool, actionFilters, indexNameExpressionResolver,
                DeleteWatchRequest::new);
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
            DeleteResponse deleteResponse = watcherService.deleteWatch(request.getId()).deleteResponse();
            boolean deleted = deleteResponse.getResult() == DocWriteResponse.Result.DELETED;
            DeleteWatchResponse response = new DeleteWatchResponse(deleteResponse.getId(), deleteResponse.getVersion(), deleted);
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
