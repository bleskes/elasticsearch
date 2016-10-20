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
import org.elasticsearch.xpack.prelert.job.results.CategoryDefinition;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;
import org.elasticsearch.xpack.prelert.validation.PaginationParamsValidator;

import java.io.IOException;

public class GetCategoryDefinitionsAction extends Action<GetCategoryDefinitionsAction.Request, GetCategoryDefinitionsAction.Response, GetCategoryDefinitionsAction.RequestBuilder> {

    public static final GetCategoryDefinitionsAction INSTANCE = new GetCategoryDefinitionsAction();
    private static final String NAME = "cluster:admin/prelert/categorydefinitions/get";

    private GetCategoryDefinitionsAction() {
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
        private int skip = 0;
        private int take = 100;

        public Request(String jobId) {
            this.jobId = ExceptionsHelper.requireNonNull(jobId, "jobId");
        }

        private Request() {
        }

        public String getJobId() {
            return jobId;
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

        @Override
        public ActionRequestValidationException validate() {
            return null;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            jobId = in.readString();
            skip = in.readInt();
            take = in.readInt();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeString(jobId);
            out.writeInt(skip);
            out.writeInt(take);
        }
    }

    public static class RequestBuilder extends ActionRequestBuilder<Request, Response, RequestBuilder> {

        public RequestBuilder(ElasticsearchClient client, GetCategoryDefinitionsAction action) {
            super(client, action, new Request());
        }
    }

    public static class Response extends ActionResponse {

        private BytesReference response;

        public Response(ObjectMapper objectMapper, QueryPage<CategoryDefinition> result) throws JsonProcessingException {
            this.response = new BytesArray(objectMapper.writeValueAsBytes(result));
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
            QueryPage<CategoryDefinition> result = jobProvider.categoryDefinitions(request.jobId, request.skip, request.take);
            try {
                listener.onResponse(new Response(objectMapper, result));
            } catch (JsonProcessingException e) {
                listener.onFailure(e);
            }
        }
    }
}
