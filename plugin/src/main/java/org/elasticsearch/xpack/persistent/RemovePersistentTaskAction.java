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
package org.elasticsearch.xpack.persistent;

import org.elasticsearch.action.Action;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.master.MasterNodeOperationRequestBuilder;
import org.elasticsearch.action.support.master.MasterNodeRequest;
import org.elasticsearch.action.support.master.TransportMasterNodeAction;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.persistent.PersistentTasksCustomMetaData.PersistentTask;

import java.io.IOException;
import java.util.Objects;

public class RemovePersistentTaskAction extends Action<RemovePersistentTaskAction.Request,
        PersistentTaskResponse,
        RemovePersistentTaskAction.RequestBuilder> {

    public static final RemovePersistentTaskAction INSTANCE = new RemovePersistentTaskAction();
    public static final String NAME = "cluster:admin/persistent/remove";

    private RemovePersistentTaskAction() {
        super(NAME);
    }

    @Override
    public RequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new RequestBuilder(client, this);
    }

    @Override
    public PersistentTaskResponse newResponse() {
        return new PersistentTaskResponse();
    }

    public static class Request extends MasterNodeRequest<Request> {

        private String taskId;

        public Request() {

        }

        public Request(String taskId) {
            this.taskId = taskId;
        }

        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            taskId = in.readString();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeString(taskId);
        }

        @Override
        public ActionRequestValidationException validate() {
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Request request = (Request) o;
            return Objects.equals(taskId, request.taskId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(taskId);
        }
    }

    public static class RequestBuilder extends MasterNodeOperationRequestBuilder<RemovePersistentTaskAction.Request,
            PersistentTaskResponse, RemovePersistentTaskAction.RequestBuilder> {

        protected RequestBuilder(ElasticsearchClient client, RemovePersistentTaskAction action) {
            super(client, action, new Request());
        }

        public final RequestBuilder setTaskId(String taskId) {
            request.setTaskId(taskId);
            return this;
        }

    }

    public static class TransportAction extends TransportMasterNodeAction<Request, PersistentTaskResponse> {

        private final PersistentTasksClusterService persistentTasksClusterService;

        @Inject
        public TransportAction(Settings settings, TransportService transportService, ClusterService clusterService,
                               ThreadPool threadPool, ActionFilters actionFilters,
                               PersistentTasksClusterService persistentTasksClusterService,
                               IndexNameExpressionResolver indexNameExpressionResolver) {
            super(settings, RemovePersistentTaskAction.NAME, transportService, clusterService, threadPool, actionFilters,
                    indexNameExpressionResolver, Request::new);
            this.persistentTasksClusterService = persistentTasksClusterService;
        }

        @Override
        protected String executor() {
            return ThreadPool.Names.MANAGEMENT;
        }

        @Override
        protected PersistentTaskResponse newResponse() {
            return new PersistentTaskResponse();
        }

        @Override
        protected ClusterBlockException checkBlock(Request request, ClusterState state) {
            // Cluster is not affected but we look up repositories in metadata
            return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_WRITE);
        }

        @Override
        protected final void masterOperation(final Request request, ClusterState state,
                                             final ActionListener<PersistentTaskResponse> listener) {
            persistentTasksClusterService.removePersistentTask(request.taskId, new ActionListener<PersistentTask<?>>() {
                @Override
                public void onResponse(PersistentTask<?> task) {
                    listener.onResponse(new PersistentTaskResponse(task));
                }

                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(e);
                }
            });
        }
    }
}


