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
import org.elasticsearch.ElasticsearchException;
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
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.prelert.PrelertServices;
import org.elasticsearch.xpack.prelert.job.ModelSnapshot;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;
import org.elasticsearch.xpack.prelert.job.persistence.JobProvider;
import org.elasticsearch.xpack.prelert.job.persistence.QueryPage;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;
import org.elasticsearch.xpack.prelert.validation.PaginationParamsValidator;

import java.io.IOException;
import java.util.Objects;

public class GetModelSnapshotsAction extends Action<GetModelSnapshotsAction.Request, GetModelSnapshotsAction.Response, GetModelSnapshotsAction.RequestBuilder> {

    public static final GetModelSnapshotsAction INSTANCE = new GetModelSnapshotsAction();
    public static final String NAME = "cluster:admin/prelert/modelsnapshots/get";

    private GetModelSnapshotsAction() {
        super(NAME);
    }

    @Override
    public GetModelSnapshotsAction.RequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new RequestBuilder(client, this);
    }

    @Override
    public GetModelSnapshotsAction.Response newResponse() {
        return new Response();
    }

    public static class Request extends ActionRequest<Request> {

        private String jobId;
        private String sort;
        private String description;
        private String start;
        private String end;
        private boolean desc;
        private int take;
        private int skip;

        private Request() {
        }

        public Request(String jobId) {
            this.jobId = Objects.requireNonNull(jobId);
        }

        public String getJobId() {
            return jobId;
        }

        @Nullable
        public String getSort() {
            return sort;
        }

        public void setSort(String sort) {
            this.sort = sort;
        }

        public boolean getDescOrder() {
            return desc;
        }

        public void setDescOrder(boolean desc) {
            this.desc = desc;
        }

        public int getTake() {
            return take;
        }

        public int getSkip() {
            return skip;
        }

        public void setPagination(int skip, int take) {
            PaginationParamsValidator.validate(skip, take);
            this.skip = skip;
            this.take = take;
        }

        @Nullable
        public String getStart() {
            return start;
        }

        public void setStart(String start) {
            this.start = start;
        }

        @Nullable
        public String getEnd() {
            return end;
        }

        public void setEnd(String end) {
            this.end = end;
        }

        @Nullable
        public String getDescriptionString() {
            return description;
        }

        public void setDescriptionString(String description) {
            this.description = description;
        }

        @Override
        public ActionRequestValidationException validate() {
            return null;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            jobId = in.readOptionalString();
            sort = in.readOptionalString();
            description = in.readOptionalString();
            start = in.readOptionalString();
            end = in.readOptionalString();
            desc = in.readBoolean();
            take = in.readVInt();
            skip = in.readVInt();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeOptionalString(jobId);
            out.writeOptionalString(sort);
            out.writeOptionalString(description);
            out.writeOptionalString(start);
            out.writeOptionalString(end);
            out.writeBoolean(desc);
            out.writeVInt(take);
            out.writeVInt(skip);
        }
    }


    public static class Response extends ActionResponse {

        private BytesReference response;

        public Response(QueryPage<ModelSnapshot> page, ObjectMapper objectMapper) throws JsonProcessingException {
            this.response = new BytesArray(objectMapper.writeValueAsString(page));
        }

        private Response() {
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

    public static class RequestBuilder extends ActionRequestBuilder<Request, Response, RequestBuilder> {

        public RequestBuilder(ElasticsearchClient client, GetModelSnapshotsAction action) {
            super(client, action, new Request());
        }
    }

    public static class TransportAction extends HandledTransportAction<Request, Response> {

        private final PrelertServices prelertServices;
        private final ObjectMapper objectMapper = new ObjectMapper();

        @Inject
        public TransportAction(Settings settings, TransportService transportService, ThreadPool threadPool,
                               ActionFilters actionFilters,IndexNameExpressionResolver indexNameExpressionResolver,
                               PrelertServices prelertServices) {
            super(settings, NAME, threadPool, transportService, actionFilters, indexNameExpressionResolver, Request::new);
            this.prelertServices = prelertServices;
        }

        @Override
        protected void doExecute(Request request, ActionListener<Response> listener) {
            logger.debug(String.format("Get model snapshots for job %s. skip = %d, take = %d"
                            + " start = '%s', end='%s', sort=%s descending=%b, description filter=%s",
                    request.getJobId(), request.getSkip(), request.getTake(), request.getStart(), request.getEnd(),
                    request.getSort(), request.getDescOrder(), request.getDescriptionString()));

            JobProvider provider = prelertServices.getJobProvider();
            QueryPage<ModelSnapshot> page;
            try {
                page = doGetPage(provider, request);
            } catch (JobException e) {
                throw ExceptionsHelper.missingException(request.getJobId());
            }

            logger.debug(String.format("Return %d model snapshots for job %s",
                    page.hitCount(), request.getJobId()));
            try {
                listener.onResponse(new Response(page, objectMapper));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        public static QueryPage<ModelSnapshot> doGetPage(JobProvider jobProvider, Request request) throws JobException {
            QueryPage<ModelSnapshot> page = jobProvider.modelSnapshots(request.getJobId(), request.getSkip(),
                    request.getTake(), request.getStart(), request.getEnd(), request.getSort(), request.getDescOrder(),
                    null, request.getDescriptionString());

            // The quantiles can be large, and totally dominate the output - it's
            // clearer to remove them
            if (page.hits() != null) {
                for (ModelSnapshot modelSnapshot : page.hits()) {
                    modelSnapshot.setQuantiles(null);
                }
            }
            return page;
        }
    }

}
