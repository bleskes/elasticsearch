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
 * Dissemination of this i  nformation or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */
package org.elasticsearch.xpack.prelert.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.Action;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.StatusToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.prelert.job.persistence.BucketQueryBuilder;
import org.elasticsearch.xpack.prelert.job.persistence.ElasticsearchJobProvider;
import org.elasticsearch.xpack.prelert.job.persistence.JobProvider;
import org.elasticsearch.xpack.prelert.job.results.Bucket;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;
import org.elasticsearch.xpack.prelert.utils.SingleDocument;

import java.io.IOException;
import java.util.Optional;

public class GetBucketAction extends Action<GetBucketAction.Request, GetBucketAction.Response, GetBucketAction.RequestBuilder> {

    public static final GetBucketAction INSTANCE = new GetBucketAction();
    public static final String NAME = "indices:admin/prelert/results/bucket/get";

    private GetBucketAction() {
        super(NAME);
    }

    @Override
    public RequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new RequestBuilder(client);
    }

    @Override
    public Response newResponse() {
        return new Response();
    }

    public static class Request extends ActionRequest<Request> {

        private String jobId;
        private String timestamp;
        private boolean expand = false;
        private boolean includeInterim = false;
        private String partitionValue;

        private Request() {
        }

        public Request(String jobId, String timestamp) {
            this.jobId = ExceptionsHelper.requireNonNull(jobId, "jobId");
            this.timestamp = ExceptionsHelper.requireNonNull(timestamp, "timestamp");
        }

        public String getJobId() {
            return jobId;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public boolean isExpand() {
            return expand;
        }

        public void setExpand(boolean expand) {
            this.expand = expand;
        }

        public boolean isIncludeInterim() {
            return includeInterim;
        }

        public void setIncludeInterim(boolean includeInterim) {
            this.includeInterim = includeInterim;
        }

        public String getPartitionValue() {
            return partitionValue;
        }

        public void setPartitionValue(String partitionValue) {
            this.partitionValue = ExceptionsHelper.requireNonNull(partitionValue, "partitionValue");
        }

        @Override
        public ActionRequestValidationException validate() {
            return null;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            jobId = in.readString();
            timestamp = in.readString();
            expand = in.readBoolean();
            includeInterim = in.readBoolean();
            partitionValue = in.readOptionalString();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeString(jobId);
            out.writeString(timestamp);
            out.writeBoolean(expand);
            out.writeBoolean(includeInterim);
            out.writeOptionalString(partitionValue);
        }
    }

    static class RequestBuilder extends ActionRequestBuilder<Request, Response, RequestBuilder> {

        RequestBuilder(ElasticsearchClient client) {
            super(client, INSTANCE, new Request());
        }
    }

    public static class Response extends ActionResponse implements StatusToXContent {

        private SingleDocument result;

        private Response() {
            result = SingleDocument.empty(Bucket.TYPE);
        }

        Response(SingleDocument result) {
            this.result = result;
        }

        public SingleDocument getResponse() {
            return result;
        }

        @Override
        public RestStatus status() {
            return result.isExists() ? RestStatus.OK : RestStatus.NOT_FOUND;
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            return result.toXContent(builder, params);
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            result = new SingleDocument(in.readString(), in.readBytesReference());
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeString(result.getType());
            out.writeBytesReference(result.getDocument());
        }
    }

    public static class TransportAction extends HandledTransportAction<Request, Response> {

        private final JobProvider jobProvider;
        private final ObjectMapper objectMapper = new ObjectMapper();

        @Inject
        public TransportAction(Settings settings, ThreadPool threadPool, TransportService transportService,
                               ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver,
                               ElasticsearchJobProvider jobProvider) {
            super(settings, NAME, threadPool, transportService, actionFilters, indexNameExpressionResolver, Request::new);
            this.jobProvider = jobProvider;
        }

        @Override
        protected void doExecute(Request request, ActionListener<Response> listener) {
            BucketQueryBuilder.BucketQuery query =
                    new BucketQueryBuilder(request.timestamp).expand(request.expand)
                            .includeInterim(request.includeInterim)
                            .partitionValue(request.partitionValue)
                            .build();

            Optional<Bucket> b = jobProvider.bucket(request.jobId, query);
            if (b.isPresent()) {
                try {
                    BytesReference document = new BytesArray(objectMapper.writeValueAsBytes(b.get()));
                    listener.onResponse(new Response(new SingleDocument(Bucket.TYPE, document)));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            } else {
                listener.onResponse(new Response());
            }
        }
    }

}
