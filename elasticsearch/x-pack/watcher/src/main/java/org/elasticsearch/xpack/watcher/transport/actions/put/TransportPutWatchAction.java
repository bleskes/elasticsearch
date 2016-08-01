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

package org.elasticsearch.xpack.watcher.transport.actions.put;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.LicenseUtils;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.watcher.Watcher;
import org.elasticsearch.xpack.watcher.WatcherService;
import org.elasticsearch.xpack.watcher.transport.actions.WatcherTransportAction;
import org.elasticsearch.xpack.watcher.watch.WatchStore;

/**
 */
public class TransportPutWatchAction extends WatcherTransportAction<PutWatchRequest, PutWatchResponse> {

    private final WatcherService watcherService;

    @Inject
    public TransportPutWatchAction(Settings settings, TransportService transportService, ClusterService clusterService,
                                   ThreadPool threadPool, ActionFilters actionFilters,
                                   IndexNameExpressionResolver indexNameExpressionResolver, WatcherService watcherService,
                                   XPackLicenseState licenseState) {
        super(settings, PutWatchAction.NAME, transportService, clusterService, threadPool, actionFilters,
            indexNameExpressionResolver, licenseState, PutWatchRequest::new);
        this.watcherService = watcherService;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.MANAGEMENT;
    }

    @Override
    protected PutWatchResponse newResponse() {
        return new PutWatchResponse();
    }

    @Override
    protected void masterOperation(PutWatchRequest request, ClusterState state, ActionListener<PutWatchResponse> listener) throws
            ElasticsearchException {
        if (licenseState.isWatcherAllowed() == false) {
            listener.onFailure(LicenseUtils.newComplianceException(Watcher.NAME));
            return;
        }

        try {
            IndexResponse indexResponse = watcherService.putWatch(request.getId(), request.getSource(), request.masterNodeTimeout(),
                    request.isActive());
            boolean created = indexResponse.getResult() == DocWriteResponse.Result.CREATED;
            listener.onResponse(new PutWatchResponse(indexResponse.getId(), indexResponse.getVersion(), created));
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }

    @Override
    protected ClusterBlockException checkBlock(PutWatchRequest request, ClusterState state) {
        return state.blocks().indexBlockedException(ClusterBlockLevel.WRITE, WatchStore.INDEX);
    }

}
