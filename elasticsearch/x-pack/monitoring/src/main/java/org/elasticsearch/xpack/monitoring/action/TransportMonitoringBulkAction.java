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

package org.elasticsearch.xpack.monitoring.action;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.AbstractRunnable;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.monitoring.agent.exporter.Exporters;
import org.elasticsearch.xpack.monitoring.agent.exporter.MonitoringDoc;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TransportMonitoringBulkAction extends HandledTransportAction<MonitoringBulkRequest, MonitoringBulkResponse> {

    private final ClusterService clusterService;
    private final Exporters exportService;

    @Inject
    public TransportMonitoringBulkAction(Settings settings, ThreadPool threadPool, ClusterService clusterService,
                                         TransportService transportService, ActionFilters actionFilters,
                                         IndexNameExpressionResolver indexNameExpressionResolver, Exporters exportService) {
        super(settings, MonitoringBulkAction.NAME, threadPool, transportService, actionFilters, indexNameExpressionResolver,
                MonitoringBulkRequest::new);
        this.clusterService = clusterService;
        this.exportService = exportService;
    }

    @Override
    protected void doExecute(MonitoringBulkRequest request, ActionListener<MonitoringBulkResponse> listener) {
        clusterService.state().blocks().globalBlockedRaiseException(ClusterBlockLevel.WRITE);
        new AsyncAction(request, listener, exportService, clusterService).start();
    }

    class AsyncAction {

        private final MonitoringBulkRequest request;
        private final ActionListener<MonitoringBulkResponse> listener;
        private final Exporters exportService;
        private final ClusterService clusterService;

        public AsyncAction(MonitoringBulkRequest request, ActionListener<MonitoringBulkResponse> listener,
                           Exporters exportService, ClusterService clusterService) {
            this.request = request;
            this.listener = listener;
            this.exportService = exportService;
            this.clusterService = clusterService;
        }

        void start() {
            executeExport(prepareForExport(request.getDocs()), System.nanoTime(), listener);
        }

        /**
         * Iterate over the documents and set the values of common fields if needed:
         * - cluster UUID
         * - timestamp
         * - source node
         */
        Collection<MonitoringDoc> prepareForExport(Collection<? extends MonitoringDoc> docs) {
            final String clusterUUID = clusterService.state().metaData().clusterUUID();
            Function<MonitoringDoc, MonitoringDoc> updateClusterUUID = doc -> {
                if (doc.getClusterUUID() == null) {
                    doc.setClusterUUID(clusterUUID);
                }
                return doc;
            };

            final long timestamp = System.currentTimeMillis();
            Function<MonitoringDoc, MonitoringDoc> updateTimestamp = doc -> {
                if (doc.getTimestamp() == 0) {
                    doc.setTimestamp(timestamp);
                }
                return doc;
            };

            final DiscoveryNode sourceNode = clusterService.localNode();
            Function<MonitoringDoc, MonitoringDoc> updateSourceNode = doc -> {
                if (doc.getSourceNode() == null) {
                    doc.setSourceNode(sourceNode);
                }
                return doc;
            };

            return docs.stream()
                    .map(updateClusterUUID.andThen(updateTimestamp.andThen(updateSourceNode)))
                    .collect(Collectors.toList());
        }

        /**
         * Exports the documents
         */
        void executeExport(final Collection<MonitoringDoc> docs, final long startTimeNanos,
                           final ActionListener<MonitoringBulkResponse> listener) {
            threadPool.generic().execute(new AbstractRunnable() {
                @Override
                protected void doRun() throws Exception {
                    exportService.export(docs);
                    listener.onResponse(new MonitoringBulkResponse(buildTookInMillis(startTimeNanos)));
                }

                @Override
                public void onFailure(Exception e) {
                    listener.onResponse(new MonitoringBulkResponse(buildTookInMillis(startTimeNanos), new MonitoringBulkResponse.Error(e)));
                }
            });
        }
    }

    private long buildTookInMillis(long startTimeNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNanos);
    }
}
