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
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParseFieldMatcherSupplier;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ObjectParser;
import org.elasticsearch.common.xcontent.StatusToXContent;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.persistence.BucketQueryBuilder;
import org.elasticsearch.xpack.prelert.job.persistence.ElasticsearchJobProvider;
import org.elasticsearch.xpack.prelert.job.persistence.JobProvider;
import org.elasticsearch.xpack.prelert.job.results.Bucket;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;
import org.elasticsearch.xpack.prelert.utils.SingleDocument;

import java.io.IOException;
import java.util.Objects;
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

    public static class Request extends ActionRequest<Request> implements ToXContent {

        public static final ParseField EXPAND = new ParseField("expand");
        public static final ParseField INCLUDE_INTERIM = new ParseField("includeInterim");
        public static final ParseField PARTITION_VALUE = new ParseField("partitionValue");

        private static final ObjectParser<Request, ParseFieldMatcherSupplier> PARSER = new ObjectParser<>(NAME, Request::new);

        static {
            PARSER.declareString((request, jobId) -> request.jobId = jobId, JobDetails.ID);
            PARSER.declareString((request, timestamp) -> request.timestamp = timestamp, Bucket.TIMESTAMP);
            PARSER.declareString(Request::setPartitionValue, PARTITION_VALUE);
            PARSER.declareBoolean(Request::setExpand, EXPAND);
            PARSER.declareBoolean(Request::setIncludeInterim, INCLUDE_INTERIM);
        }

        public static Request parseRequest(String jobId, String timestamp, XContentParser parser,
                ParseFieldMatcherSupplier parseFieldMatcherSupplier) {
            Request request = PARSER.apply(parser, parseFieldMatcherSupplier);
            if (jobId != null) {
                request.jobId = jobId;
            }
            if (timestamp != null) {
                request.timestamp = timestamp;
            }
            return request;
        }

        private String jobId;
        private String timestamp;
        private boolean expand = false;
        private boolean includeInterim = false;
        private String partitionValue;

        Request() {
        }

        public Request(String jobId, String timestamp) {
            this.jobId = ExceptionsHelper.requireNonNull(jobId, JobDetails.ID.getPreferredName());
            this.timestamp = ExceptionsHelper.requireNonNull(timestamp, Bucket.TIMESTAMP.getPreferredName());
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
            this.partitionValue = ExceptionsHelper.requireNonNull(partitionValue, PARTITION_VALUE.getPreferredName());
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

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            builder.field(JobDetails.ID.getPreferredName(), jobId);
            builder.field(Bucket.TIMESTAMP.getPreferredName(), timestamp);
            builder.field(EXPAND.getPreferredName(), expand);
            builder.field(INCLUDE_INTERIM.getPreferredName(), includeInterim);
            if (partitionValue != null) {
                builder.field(PARTITION_VALUE.getPreferredName(), partitionValue);
            }
            builder.endObject();
            return builder;
        }

        @Override
        public int hashCode() {
            return Objects.hash(jobId, timestamp, partitionValue, expand, includeInterim);
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
            return Objects.equals(jobId, other.jobId) &&
                    Objects.equals(timestamp, other.timestamp) &&
                    Objects.equals(partitionValue, other.partitionValue) &&
                    Objects.equals(expand, other.expand) &&
                    Objects.equals(includeInterim, other.includeInterim);
        }
    }

    static class RequestBuilder extends ActionRequestBuilder<Request, Response, RequestBuilder> {

        RequestBuilder(ElasticsearchClient client) {
            super(client, INSTANCE, new Request());
        }
    }

    public static class Response extends ActionResponse implements StatusToXContent {

        private SingleDocument<Bucket> result;

        Response() {
            result = SingleDocument.empty(Bucket.TYPE.getPreferredName());
        }

        Response(SingleDocument<Bucket> result) {
            this.result = result;
        }

        public SingleDocument<Bucket> getResponse() {
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
            result = new SingleDocument<>(in, Bucket::new);
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            result.writeTo(out);
        }

        @Override
        public int hashCode() {
            return Objects.hash(result);
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
            return Objects.equals(result, other.result);
        }
    }

    public static class TransportAction extends HandledTransportAction<Request, Response> {

        private final JobProvider jobProvider;

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
                listener.onResponse(new Response(new SingleDocument<>(Bucket.TYPE.getPreferredName(), b.get())));
            } else {
                listener.onResponse(new Response());
            }
        }
    }

}
