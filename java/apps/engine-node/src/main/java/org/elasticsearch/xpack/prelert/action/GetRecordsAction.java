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
import org.elasticsearch.xpack.prelert.job.persistence.ElasticsearchJobProvider;
import org.elasticsearch.xpack.prelert.job.persistence.JobProvider;
import org.elasticsearch.xpack.prelert.job.persistence.QueryPage;
import org.elasticsearch.xpack.prelert.job.persistence.RecordsQueryBuilder;
import org.elasticsearch.xpack.prelert.job.results.AnomalyRecord;
import org.elasticsearch.xpack.prelert.job.results.Influencer;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;
import org.elasticsearch.xpack.prelert.validation.PaginationParamsValidator;

import java.io.IOException;

public class GetRecordsAction extends Action<GetRecordsAction.Request, GetRecordsAction.Response, GetRecordsAction.RequestBuilder> {

    public static final GetRecordsAction INSTANCE = new GetRecordsAction();
    public static final String NAME = "indices:admin/prelert/results/records/get";

    private GetRecordsAction() {
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
        private String start;
        private String end;
        private boolean includeInterim = false;
        private int skip = 0;
        private int take = 100;
        private double anomalyScoreFilter = 0.0;
        private String sort = Influencer.ANOMALY_SCORE;
        private boolean decending = false;
        private double maxNormalizedProbability = 0.0;
        private String partitionValue;

        private Request() {
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

        public boolean isDecending() {
            return decending;
        }

        public void setDecending(boolean decending) {
            this.decending = decending;
        }

        public boolean isIncludeInterim() {
            return includeInterim;
        }

        public void setIncludeInterim(boolean includeInterim) {
            this.includeInterim = includeInterim;
        }

        public int getSkip() {
            return skip;
        }

        public int getTake() {
            return take;
        }

        public void setPagination(int skip, int take) {
            PaginationParamsValidator.validate(skip, take);
            this.skip = skip;
            this.take = take;
        }

        public double getAnomalyScoreFilter() {
            return anomalyScoreFilter;
        }

        public void setAnomalyScore(double anomalyScoreFilter) {
            this.anomalyScoreFilter = anomalyScoreFilter;
        }

        public String getSort() {
            return sort;
        }

        public void setSort(String sort) {
            this.sort = ExceptionsHelper.requireNonNull(sort, "sort");
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
            includeInterim = in.readBoolean();
            skip = in.readInt();
            take = in.readInt();
            start = in.readString();
            end = in.readString();
            sort = in.readOptionalString();
            decending = in.readBoolean();
            anomalyScoreFilter = in.readDouble();
            maxNormalizedProbability = in.readDouble();
            partitionValue = in.readOptionalString();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeString(jobId);
            out.writeBoolean(includeInterim);
            out.writeInt(skip);
            out.writeInt(take);
            out.writeString(start);
            out.writeString(end);
            out.writeOptionalString(sort);
            out.writeBoolean(decending);
            out.writeDouble(anomalyScoreFilter);
            out.writeDouble(maxNormalizedProbability);
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
        }

        Response(QueryPage<AnomalyRecord> records, ObjectMapper objectMapper) throws JsonProcessingException {
            response = new BytesArray(objectMapper.writeValueAsBytes(records));
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
            RecordsQueryBuilder.RecordsQuery query = new RecordsQueryBuilder()
                    .includeInterim(request.includeInterim)
                    .epochStart(request.start)
                    .epochEnd(request.end)
                    .skip(request.skip)
                    .take(request.take)
                    .anomalyScoreThreshold(request.anomalyScoreFilter)
                    .sortField(request.sort)
                    .sortDescending(request.decending)
                    .build();

            try {
                QueryPage<AnomalyRecord> page = jobProvider.records(request.jobId, query);
                listener.onResponse(new Response(page, objectMapper));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
