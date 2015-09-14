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

package org.elasticsearch.watcher.transport.actions.get;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.watcher.WatcherService;
import org.elasticsearch.watcher.license.LicenseService;
import org.elasticsearch.watcher.support.xcontent.WatcherParams;
import org.elasticsearch.watcher.transport.actions.WatcherTransportAction;
import org.elasticsearch.watcher.watch.Watch;
import org.elasticsearch.watcher.watch.WatchStore;

import java.io.IOException;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * Performs the get operation.
 */
public class TransportGetWatchAction extends WatcherTransportAction<GetWatchRequest, GetWatchResponse> {

    private final WatcherService watcherService;

    @Inject
    public TransportGetWatchAction(Settings settings, TransportService transportService, ClusterService clusterService,
                                   ThreadPool threadPool, ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver,
                                   WatcherService watcherService, LicenseService licenseService) {
        super(settings, GetWatchAction.NAME, transportService, clusterService, threadPool, actionFilters, indexNameExpressionResolver, licenseService, GetWatchRequest::new);
        this.watcherService = watcherService;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.SAME; // Super lightweight operation, so don't fork
    }

    @Override
    protected GetWatchResponse newResponse() {
        return new GetWatchResponse();
    }

    @Override
    protected void masterOperation(GetWatchRequest request, ClusterState state, ActionListener<GetWatchResponse> listener) throws ElasticsearchException {
        try {
            Watch watch = watcherService.getWatch(request.getId());
            if (watch == null) {
                listener.onResponse(new GetWatchResponse(request.getId()));
                return;
            }

            try (XContentBuilder builder = jsonBuilder()) {
                // When we return the watch via the get api, we want to return the watch as was specified in the put api,
                // we don't include the status in the watch source itself, but as a separate top level field, so that
                // it indicates the the status is managed by watcher itself.
                watch.toXContent(builder, WatcherParams.builder().hideSecrets(true).build());
                BytesReference watchSource = builder.bytes();
                listener.onResponse(new GetWatchResponse(watch.id(), watch.status(), watchSource, XContentType.JSON));
            } catch (IOException e) {
                listener.onFailure(e);
            }

        } catch (Throwable t) {
            logger.error("failed to get watch [{}]", t, request.getId());
            throw t;
        }
    }

    @Override
    protected ClusterBlockException checkBlock(GetWatchRequest request, ClusterState state) {
        return state.blocks().indexBlockedException(ClusterBlockLevel.READ, WatchStore.INDEX);
    }
}
