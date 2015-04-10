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

package org.elasticsearch.watcher.transport.actions.put;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.watcher.license.LicenseService;
import org.elasticsearch.watcher.transport.actions.WatcherTransportAction;
import org.elasticsearch.watcher.watch.WatchService;
import org.elasticsearch.watcher.watch.WatchStore;

/**
 */
public class TransportPutWatchAction extends WatcherTransportAction<PutWatchRequest, PutWatchResponse> {

    private final WatchService watchService;

    @Inject
    public TransportPutWatchAction(Settings settings, TransportService transportService, ClusterService clusterService,
                                   ThreadPool threadPool, ActionFilters actionFilters, WatchService watchService, LicenseService licenseService) {
        super(settings, PutWatchAction.NAME, transportService, clusterService, threadPool, actionFilters, licenseService);
        this.watchService = watchService;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.MANAGEMENT;
    }

    @Override
    protected PutWatchRequest newRequest() {
        return new PutWatchRequest();
    }

    @Override
    protected PutWatchResponse newResponse() {
        return new PutWatchResponse();
    }

    @Override
    protected void masterOperation(PutWatchRequest request, ClusterState state, ActionListener<PutWatchResponse> listener) throws ElasticsearchException {
        try {
            IndexResponse indexResponse = watchService.putWatch(request.getId(), request.getSource());
            listener.onResponse(new PutWatchResponse(indexResponse.getId(), indexResponse.getVersion(), indexResponse.isCreated()));
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }

    @Override
    protected ClusterBlockException checkBlock(PutWatchRequest request, ClusterState state) {
        request.beforeLocalFork(); // This is the best place to make the watch source safe
        return state.blocks().indexBlockedException(ClusterBlockLevel.WRITE, WatchStore.INDEX);
    }

}
