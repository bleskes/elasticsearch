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
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.persistence.BucketsQueryBuilder;
import org.elasticsearch.xpack.prelert.job.persistence.ElasticsearchJobProvider;
import org.elasticsearch.xpack.prelert.job.persistence.JobProvider;
import org.elasticsearch.xpack.prelert.job.persistence.QueryPage;
import org.elasticsearch.xpack.prelert.job.results.Bucket;
import org.elasticsearch.xpack.prelert.job.results.PageParams;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

import java.io.IOException;
import java.util.Objects;

public class GetBucketsAction extends Action<GetBucketsAction.Request, GetBucketsAction.Response, GetBucketsAction.RequestBuilder> {

    public static final GetBucketsAction INSTANCE = new GetBucketsAction();
    public static final String NAME = "indices:admin/prelert/results/buckets/get";

    private GetBucketsAction() {
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

        public static final ParseField START = new ParseField("start");
        public static final ParseField END = new ParseField("end");
        public static final ParseField EXPAND = new ParseField("expand");
        public static final ParseField INCLUDE_INTERIM = new ParseField("includeInterim");
        public static final ParseField ANOMALY_SCORE = new ParseField("anomalyScore");
        public static final ParseField MAX_NORMALIZED_PROBABILITY = new ParseField("maxNormalizedProbability");
        public static final ParseField PARTITION_VALUE = new ParseField("partitionValue");

        private static final ObjectParser<Request, ParseFieldMatcherSupplier> PARSER = new ObjectParser<>(NAME, Request::new);

        static {
            PARSER.declareString((request, jobId) -> request.jobId = jobId, JobDetails.ID);
            PARSER.declareString((request, start) -> request.start = start, START);
            PARSER.declareString((request, end) -> request.end = end, END);
            PARSER.declareBoolean(Request::setExpand, EXPAND);
            PARSER.declareBoolean(Request::setIncludeInterim, INCLUDE_INTERIM);
            PARSER.declareObject(Request::setPageParams, PageParams.PARSER, PageParams.PAGE);
            PARSER.declareDouble(Request::setAnomalyScore, ANOMALY_SCORE);
            PARSER.declareDouble(Request::setMaxNormalizedProbability, MAX_NORMALIZED_PROBABILITY);
            PARSER.declareString(Request::setPartitionValue, PARTITION_VALUE);
        }

        public static Request parseRequest(String jobId, XContentParser parser, ParseFieldMatcherSupplier parseFieldMatcherSupplier) {
            Request request = PARSER.apply(parser, parseFieldMatcherSupplier);
            if (jobId != null) {
                request.jobId = jobId;
            }
            return request;
        }

        private String jobId;
        private String start;
        private String end;
        private boolean expand = false;
        private boolean includeInterim = false;
        private PageParams pageParams = new PageParams(0, 100);
        private double anomalyScore = 0.0;
        private double maxNormalizedProbability = 0.0;
        private String partitionValue;

        Request() {
        }

        public Request(String jobId, String start, String end) {
            this.jobId = ExceptionsHelper.requireNonNull(jobId, "jobId");
            this.start = ExceptionsHelper.requireNonNull(start, "start");
            this.end = ExceptionsHelper.requireNonNull(end, "end");
        }

        public String getJobId() {
            return jobId;
        }

        public String getStart() {
            return start;
        }

        public String getEnd() {
            return end;
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

        public PageParams getPageParams() {
            return pageParams;
        }

        public void setPageParams(PageParams pageParams) {
            this.pageParams = pageParams;
        }

        public double getAnomalyScore() {
            return anomalyScore;
        }

        public void setAnomalyScore(double anomalyScore) {
            this.anomalyScore = anomalyScore;
        }

        public double getMaxNormalizedProbability() {
            return maxNormalizedProbability;
        }

        public void setMaxNormalizedProbability(double maxNormalizedProbability) {
            this.maxNormalizedProbability = maxNormalizedProbability;
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
            expand = in.readBoolean();
            includeInterim = in.readBoolean();
            pageParams = new PageParams(in);
            start = in.readString();
            end = in.readString();
            anomalyScore = in.readDouble();
            maxNormalizedProbability = in.readDouble();
            partitionValue = in.readOptionalString();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeString(jobId);
            out.writeBoolean(expand);
            out.writeBoolean(includeInterim);
            pageParams.writeTo(out);
            out.writeString(start);
            out.writeString(end);
            out.writeDouble(anomalyScore);
            out.writeDouble(maxNormalizedProbability);
            out.writeOptionalString(partitionValue);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            builder.field(JobDetails.ID.getPreferredName(), jobId);
            builder.field(START.getPreferredName(), start);
            builder.field(END.getPreferredName(), end);
            builder.field(EXPAND.getPreferredName(), expand);
            builder.field(INCLUDE_INTERIM.getPreferredName(), includeInterim);
            builder.field(PageParams.PAGE.getPreferredName(), pageParams);
            builder.field(ANOMALY_SCORE.getPreferredName(), anomalyScore);
            builder.field(MAX_NORMALIZED_PROBABILITY.getPreferredName(), maxNormalizedProbability);
            if (partitionValue != null) {
                builder.field(PARTITION_VALUE.getPreferredName(), partitionValue);
            }
            builder.endObject();
            return builder;
        }

        @Override
        public int hashCode() {
            return Objects.hash(jobId, start, end, expand, includeInterim, pageParams, anomalyScore, maxNormalizedProbability,
                    partitionValue);
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
                    Objects.equals(start, other.start) &&
                    Objects.equals(end, other.end) &&
                    Objects.equals(expand, other.expand) &&
                    Objects.equals(includeInterim, other.includeInterim) &&
                    Objects.equals(pageParams, other.pageParams) &&
                    Objects.equals(anomalyScore, other.anomalyScore) &&
                    Objects.equals(maxNormalizedProbability, other.maxNormalizedProbability) &&
                    Objects.equals(partitionValue, other.partitionValue);
        }

        @SuppressWarnings("deprecation")
        @Override
        public final String toString() {
            try {
                XContentBuilder builder = XContentFactory.jsonBuilder();
                builder.prettyPrint();
                toXContent(builder, EMPTY_PARAMS);
                return builder.string();
            } catch (Exception e) {
                // So we have a stack trace logged somewhere
                return "{ \"error\" : \"" + org.elasticsearch.ExceptionsHelper.detailedMessage(e) + "\"}";
            }
        }
    }

    static class RequestBuilder extends ActionRequestBuilder<Request, Response, RequestBuilder> {

        RequestBuilder(ElasticsearchClient client) {
            super(client, INSTANCE, new Request());
        }
    }

    public static class Response extends ActionResponse implements ToXContent {

        private QueryPage<Bucket> buckets;

        Response() {
        }

        Response(QueryPage<Bucket> buckets) {
            this.buckets = buckets;
        }

        public QueryPage<Bucket> getBuckets() {
            return buckets;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            buckets = new QueryPage<>(in, Bucket::new);
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            buckets.writeTo(out);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            return buckets.doXContentBody(builder, params);
        }

        @Override
        public int hashCode() {
            return Objects.hash(buckets);
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
            return Objects.equals(buckets, other.buckets);
        }

        @SuppressWarnings("deprecation")
        @Override
        public final String toString() {
            try {
                XContentBuilder builder = XContentFactory.jsonBuilder();
                builder.prettyPrint();
                toXContent(builder, EMPTY_PARAMS);
                return builder.string();
            } catch (Exception e) {
                // So we have a stack trace logged somewhere
                return "{ \"error\" : \"" + org.elasticsearch.ExceptionsHelper.detailedMessage(e) + "\"}";
            }
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
            BucketsQueryBuilder.BucketsQuery query =
                    new BucketsQueryBuilder().expand(request.expand)
                    .includeInterim(request.includeInterim)
                    .epochStart(request.start)
                    .epochEnd(request.end)
                    .skip(request.pageParams.getSkip())
                    .take(request.pageParams.getTake())
                    .anomalyScoreThreshold(request.anomalyScore)
                    .normalizedProbabilityThreshold(request.maxNormalizedProbability)
                    .partitionValue(request.partitionValue)
                    .build();

            QueryPage<Bucket> page = jobProvider.buckets(request.jobId, query);
            listener.onResponse(new Response(page));
        }
    }

}
