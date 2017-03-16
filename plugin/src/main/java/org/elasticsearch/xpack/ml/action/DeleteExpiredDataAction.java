/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2017 Elasticsearch BV. All Rights Reserved.
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
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.ml.job.retention.ExpiredModelSnapshotsRemover;
import org.elasticsearch.xpack.ml.job.retention.ExpiredResultsRemover;
import org.elasticsearch.xpack.ml.notifications.Auditor;

import java.io.IOException;
import java.util.Objects;

public class DeleteExpiredDataAction extends Action<DeleteExpiredDataAction.Request, DeleteExpiredDataAction.Response,
        DeleteExpiredDataAction.RequestBuilder> {

    public static final DeleteExpiredDataAction INSTANCE = new DeleteExpiredDataAction();
    public static final String NAME = "cluster:admin/ml/delete_expired_data";

    private DeleteExpiredDataAction() {
        super(NAME);
    }

    @Override
    public RequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new RequestBuilder(client, this);
    }

    @Override
    public Response newResponse() {
        return new Response();
    }

    public static class Request extends ActionRequest {

        public Request() {}

        @Override
        public ActionRequestValidationException validate() {
            return null;
        }
    }

    static class RequestBuilder extends ActionRequestBuilder<Request, Response, RequestBuilder> {

        RequestBuilder(ElasticsearchClient client, DeleteExpiredDataAction action) {
            super(client, action, new Request());
        }
    }

    public static class Response extends ActionResponse implements ToXContentObject {

        private static final ParseField DELETED = new ParseField("deleted");

        private boolean deleted;

        public Response(boolean deleted) {
            this.deleted = deleted;
        }

        Response() {}

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            deleted = in.readBoolean();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeBoolean(deleted);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            builder.field(DELETED.getPreferredName(), deleted);
            builder.endObject();
            return builder;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Response response = (Response) o;
            return Objects.equals(deleted, response.deleted);
        }

        @Override
        public int hashCode() {
            return Objects.hash(deleted);
        }
    }

    public static class TransportAction extends HandledTransportAction<Request, Response> {

        private final Client client;
        private final ClusterService clusterService;

        @Inject
        public TransportAction(Settings settings, ThreadPool threadPool, TransportService transportService,
                               ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver, Client client,
                               ClusterService clusterService) {
            super(settings, NAME, threadPool, transportService, actionFilters, indexNameExpressionResolver, Request::new);
            this.client = client;
            this.clusterService = clusterService;
        }

        @Override
        protected void doExecute(Request request, ActionListener<Response> listener) {
            logger.info("Deleting expired data");
            threadPool.executor(ThreadPool.Names.MANAGEMENT).execute(() -> deleteExpiredData(listener));
        }

        private void deleteExpiredData(ActionListener<Response> listener) {
            Auditor auditor = new Auditor(client, clusterService);
            ExpiredResultsRemover resultsRemover = new ExpiredResultsRemover(client, clusterService, auditor);
            resultsRemover.trigger(() -> {
                ExpiredModelSnapshotsRemover modelSnapshotsRemover = new ExpiredModelSnapshotsRemover(client, clusterService);
                modelSnapshotsRemover.trigger(() -> {
                    logger.debug("Finished deleting expired data");
                    listener.onResponse(new Response(true));
                });
            });
        }
    }
}
