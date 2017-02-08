/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */

package org.elasticsearch.xpack.ml.action;

import org.elasticsearch.action.Action;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.byscroll.AbstractBulkByScrollRequestBuilder;
import org.elasticsearch.action.bulk.byscroll.AsyncDeleteByQueryAction;
import org.elasticsearch.action.bulk.byscroll.BulkByScrollParallelizationHelper;
import org.elasticsearch.action.bulk.byscroll.BulkByScrollResponse;
import org.elasticsearch.action.bulk.byscroll.DeleteByQueryRequest;
import org.elasticsearch.action.bulk.byscroll.ParentBulkByScrollTask;
import org.elasticsearch.action.bulk.byscroll.WorkingBulkByScrollTask;
import org.elasticsearch.action.search.SearchAction;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.client.ParentTaskAssigningClient;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

public class MlDeleteByQueryAction extends Action<DeleteByQueryRequest, BulkByScrollResponse,
        MlDeleteByQueryAction.MlDeleteByQueryRequestBuilder> {

    public static final MlDeleteByQueryAction INSTANCE = new MlDeleteByQueryAction();
    public static final String NAME = "indices:data/write/delete/mlbyquery";

    private MlDeleteByQueryAction() {
        super(NAME);
    }

    @Override
    public MlDeleteByQueryRequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new MlDeleteByQueryRequestBuilder(client, this);
    }

    @Override
    public BulkByScrollResponse newResponse() {
        return new BulkByScrollResponse();
    }

    public static class MlDeleteByQueryRequestBuilder extends
            AbstractBulkByScrollRequestBuilder<DeleteByQueryRequest, MlDeleteByQueryRequestBuilder> {

        public MlDeleteByQueryRequestBuilder(ElasticsearchClient client,
                                           Action<DeleteByQueryRequest, BulkByScrollResponse, MlDeleteByQueryRequestBuilder> action) {
            this(client, action, new SearchRequestBuilder(client, SearchAction.INSTANCE));
        }

        private MlDeleteByQueryRequestBuilder(ElasticsearchClient client,
                                            Action<DeleteByQueryRequest, BulkByScrollResponse, MlDeleteByQueryRequestBuilder> action,
                                            SearchRequestBuilder search) {
            super(client, action, search, new DeleteByQueryRequest(search.request()));
        }

        @Override
        protected MlDeleteByQueryRequestBuilder self() {
            return this;
        }

        @Override
        public MlDeleteByQueryRequestBuilder abortOnVersionConflict(boolean abortOnVersionConflict) {
            request.setAbortOnVersionConflict(abortOnVersionConflict);
            return this;
        }
    }

    public static class TransportAction extends HandledTransportAction<DeleteByQueryRequest, BulkByScrollResponse> {
        private final Client client;
        private final ScriptService scriptService;
        private final ClusterService clusterService;

        @Inject
        public TransportAction(Settings settings, ThreadPool threadPool, ActionFilters actionFilters,
                                            IndexNameExpressionResolver resolver, Client client, TransportService transportService,
                                            ScriptService scriptService, ClusterService clusterService) {
            super(settings, MlDeleteByQueryAction.NAME, threadPool, transportService, actionFilters, resolver, DeleteByQueryRequest::new);
            this.client = client;
            this.scriptService = scriptService;
            this.clusterService = clusterService;
        }

        @Override
        public void doExecute(Task task, DeleteByQueryRequest request, ActionListener<BulkByScrollResponse> listener) {
            if (request.getSlices() > 1) {
                BulkByScrollParallelizationHelper.startSlices(client, taskManager, MlDeleteByQueryAction.INSTANCE,
                        clusterService.localNode().getId(), (ParentBulkByScrollTask) task, request, listener);
            } else {
                ClusterState state = clusterService.state();
                ParentTaskAssigningClient client = new ParentTaskAssigningClient(this.client, clusterService.localNode(), task);
                new AsyncDeleteByQueryAction((WorkingBulkByScrollTask) task, logger, client, threadPool, request, scriptService, state,
                        listener).start();
            }
        }

        @Override
        protected void doExecute(DeleteByQueryRequest request, ActionListener<BulkByScrollResponse> listener) {
            throw new UnsupportedOperationException("task required");
        }
    }
}
