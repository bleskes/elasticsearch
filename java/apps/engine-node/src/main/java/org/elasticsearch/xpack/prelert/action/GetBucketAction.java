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
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.prelert.PrelertServices;
import org.elasticsearch.xpack.prelert.job.persistence.BucketQueryBuilder;
import org.elasticsearch.xpack.prelert.job.persistence.JobProvider;
import org.elasticsearch.xpack.prelert.job.results.Bucket;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

public class GetBucketAction extends Action<GetBucketAction.Request, GetBucketAction.Response, GetBucketAction.RequestBuilder> {

    public static final GetBucketAction INSTANCE = new GetBucketAction();
    public static final String NAME = "indices:admin/prelert/results/bucket/get";

    public GetBucketAction() {
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
            this.jobId = Objects.requireNonNull(jobId);
            this.timestamp = Objects.requireNonNull(timestamp);
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
            this.partitionValue = Objects.requireNonNull(partitionValue);
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

    public static class Response extends ActionResponse {

        private BytesReference response;

        Response() {
            response = new BytesArray("");
        }

        Response(Bucket bucket, ObjectMapper objectMapper) throws JsonProcessingException {
            response = new BytesArray(objectMapper.writeValueAsBytes(bucket));
        }

        public BytesReference getResponse() {
            return response;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            response = in.readBytesReference();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeBytesReference(response);
        }
    }

    public static class TransportAction extends HandledTransportAction<Request, Response> {

        private final PrelertServices prelertServices;
        private final ObjectMapper objectMapper = new ObjectMapper();

        @Inject
        public TransportAction(Settings settings, ThreadPool threadPool, TransportService transportService,
                               ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver,
                               PrelertServices prelertServices) {
            super(settings, NAME, threadPool, transportService, actionFilters, indexNameExpressionResolver, Request::new);
            this.prelertServices = prelertServices;
        }

        @Override
        protected void doExecute(Request request, ActionListener<Response> listener) {
            JobProvider provider = prelertServices.getJobProvider();
            BucketQueryBuilder.BucketQuery query =
                    new BucketQueryBuilder(request.timestamp).expand(request.expand)
                            .includeInterim(request.includeInterim)
                            .partitionValue(request.partitionValue)
                            .build();

            // TODO: I don't think we should use SingleDocument here,
            // once we moved away from jackson-databind the rest layer can send the appropiate response code.
            // NORELEASE: response code delegation
            Optional<Bucket> b = provider.bucket(request.jobId, query);
            if (b.isPresent()) {
                try {
                    listener.onResponse(new Response(b.get(), objectMapper));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            } else {
                listener.onResponse(new Response());
            }
        }
    }

}
