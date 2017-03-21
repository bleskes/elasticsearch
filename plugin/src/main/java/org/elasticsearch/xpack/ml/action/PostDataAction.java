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
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.tasks.BaseTasksResponse;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.StatusToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.ml.MachineLearning;
import org.elasticsearch.xpack.ml.job.config.DataDescription;
import org.elasticsearch.xpack.ml.job.process.autodetect.AutodetectProcessManager;
import org.elasticsearch.xpack.ml.job.process.autodetect.params.DataLoadParams;
import org.elasticsearch.xpack.ml.job.process.autodetect.params.TimeRange;
import org.elasticsearch.xpack.ml.job.process.autodetect.state.DataCounts;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

public class PostDataAction extends Action<PostDataAction.Request, PostDataAction.Response, PostDataAction.RequestBuilder> {

    public static final PostDataAction INSTANCE = new PostDataAction();
    public static final String NAME = "cluster:admin/ml/job/data/post";

    private PostDataAction() {
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

    static class RequestBuilder extends ActionRequestBuilder<Request, Response, RequestBuilder> {

        RequestBuilder(ElasticsearchClient client, PostDataAction action) {
            super(client, action, new Request());
        }
    }

    public static class Response extends BaseTasksResponse implements StatusToXContentObject, Writeable {

        private DataCounts dataCounts;

        Response(String jobId) {
            dataCounts = new DataCounts(jobId);
        }

        private Response() {
        }

        public Response(DataCounts counts) {
            super(null, null);
            this.dataCounts = counts;
        }

        public DataCounts getDataCounts() {
            return dataCounts;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            dataCounts = new DataCounts(in);
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            dataCounts.writeTo(out);
        }

        @Override
        public RestStatus status() {
            return RestStatus.ACCEPTED;
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            dataCounts.doXContentBody(builder, params);
            builder.endObject();
            return builder;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(dataCounts);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Response other = (Response) obj;

            return Objects.equals(dataCounts, other.dataCounts);

        }
    }

    public static class Request extends TransportJobTaskAction.JobTaskRequest<Request> {

        public static final ParseField RESET_START = new ParseField("reset_start");
        public static final ParseField RESET_END = new ParseField("reset_end");

        private String resetStart = "";
        private String resetEnd = "";
        private DataDescription dataDescription;
        private BytesReference content;

        Request() {
        }

        public Request(String jobId) {
            super(jobId);
        }

        public String getResetStart() {
            return resetStart;
        }

        public void setResetStart(String resetStart) {
            this.resetStart = resetStart;
        }

        public String getResetEnd() {
            return resetEnd;
        }

        public void setResetEnd(String resetEnd) {
            this.resetEnd = resetEnd;
        }

        public DataDescription getDataDescription() {
            return dataDescription;
        }

        public void setDataDescription(DataDescription dataDescription) {
            this.dataDescription = dataDescription;
        }

        public BytesReference getContent() { return content; }

        public void setContent(BytesReference content) {
            this.content = content;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            resetStart = in.readOptionalString();
            resetEnd = in.readOptionalString();
            dataDescription = in.readOptionalWriteable(DataDescription::new);
            content = in.readBytesReference();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeOptionalString(resetStart);
            out.writeOptionalString(resetEnd);
            out.writeOptionalWriteable(dataDescription);
            out.writeBytesReference(content);
        }

        @Override
        public int hashCode() {
            // content stream not included
            return Objects.hash(jobId, resetStart, resetEnd, dataDescription);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Request other = (Request) obj;

            // content stream not included
            return Objects.equals(jobId, other.jobId) &&
                    Objects.equals(resetStart, other.resetStart) &&
                    Objects.equals(resetEnd, other.resetEnd) &&
                    Objects.equals(dataDescription, other.dataDescription);
        }
    }


    public static class TransportAction extends TransportJobTaskAction<OpenJobAction.JobTask, Request, Response> {

        @Inject
        public TransportAction(Settings settings, TransportService transportService, ThreadPool threadPool, ClusterService clusterService,
                ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver,
                               AutodetectProcessManager processManager) {
            super(settings, PostDataAction.NAME, threadPool, clusterService, transportService, actionFilters, indexNameExpressionResolver,
                    Request::new, Response::new, MachineLearning.THREAD_POOL_NAME, processManager);
        }

        @Override
        protected Response readTaskResponse(StreamInput in) throws IOException {
            Response response = new Response();
            response.readFrom(in);
            return response;
        }

        @Override
        protected void innerTaskOperation(Request request, OpenJobAction.JobTask task, ActionListener<Response> listener) {
            TimeRange timeRange = TimeRange.builder().startTime(request.getResetStart()).endTime(request.getResetEnd()).build();
            DataLoadParams params = new DataLoadParams(timeRange, Optional.ofNullable(request.getDataDescription()));
            try {
                DataCounts dataCounts = processManager.processData(request.getJobId(), request.content.streamInput(), params);
                listener.onResponse(new Response(dataCounts));
            } catch (Exception e) {
                listener.onFailure(e);
            }
        }

    }
}
