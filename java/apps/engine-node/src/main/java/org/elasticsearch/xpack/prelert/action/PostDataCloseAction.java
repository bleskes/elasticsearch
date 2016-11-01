/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
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
package org.elasticsearch.xpack.prelert.action;

import org.elasticsearch.action.Action;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.prelert.job.manager.AutodetectProcessManager;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

import java.io.IOException;
import java.util.Objects;

public class PostDataCloseAction extends Action<PostDataCloseAction.Request, PostDataCloseAction.Response,
PostDataCloseAction.RequestBuilder> {

    public static final PostDataCloseAction INSTANCE = new PostDataCloseAction();
    public static final String NAME = "cluster:admin/prelert/data/post/close";

    private PostDataCloseAction() {
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

    public static class Request extends ActionRequest<Request> {

        private String jobId;

        Request() {}

        public Request(String jobId) {
            this.jobId = ExceptionsHelper.requireNonNull(jobId, "jobId");
        }

        public String getJobId() {
            return jobId;
        }

        @Override
        public ActionRequestValidationException validate() {
            return null;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            jobId = in.readString();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeString(jobId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(jobId);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || obj.getClass() != getClass()) {
                return false;
            }
            Request other = (Request) obj;
            return Objects.equals(jobId, other.jobId);
        }
    }

    static class RequestBuilder extends ActionRequestBuilder<Request, Response, RequestBuilder> {

        public RequestBuilder(ElasticsearchClient client, PostDataCloseAction action) {
            super(client, action, new Request());
        }
    }

    public static class Response extends AcknowledgedResponse {

        private Response() {
        }

        private Response(boolean acknowledged) {
            super(acknowledged);
        }
    }

    // NORELEASE This should be a master node operation that updates the job's state
    public static class TransportAction extends HandledTransportAction<Request, Response> {

        private final AutodetectProcessManager processManager;

        @Inject
        public TransportAction(Settings settings, TransportService transportService, ThreadPool threadPool, ActionFilters actionFilters,
                IndexNameExpressionResolver indexNameExpressionResolver, AutodetectProcessManager processManager) {
            super(settings, PostDataCloseAction.NAME, false, threadPool, transportService, actionFilters,
                    indexNameExpressionResolver, Request::new);

            this.processManager = processManager;
        }

        @Override
        protected final void doExecute(Request request, ActionListener<Response> listener) {
            processManager.closeJob(request.getJobId());
            listener.onResponse(new Response(true));
        }
    }
}

