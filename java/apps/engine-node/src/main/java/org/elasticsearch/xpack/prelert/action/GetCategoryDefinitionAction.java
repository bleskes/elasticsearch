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
import org.elasticsearch.common.xcontent.StatusToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.prelert.job.persistence.ElasticsearchJobProvider;
import org.elasticsearch.xpack.prelert.job.persistence.JobProvider;
import org.elasticsearch.xpack.prelert.job.results.CategoryDefinition;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;
import org.elasticsearch.xpack.prelert.utils.SingleDocument;

import java.io.IOException;
import java.util.Optional;

public class GetCategoryDefinitionAction extends Action<GetCategoryDefinitionAction.Request, GetCategoryDefinitionAction.Response, GetCategoryDefinitionAction.RequestBuilder> {

    public static final GetCategoryDefinitionAction INSTANCE = new GetCategoryDefinitionAction();
    private static final String NAME = "cluster:admin/prelert/categorydefinition/get";

    private GetCategoryDefinitionAction() {
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
        private String categoryId;

        public Request(String jobId, String categoryId) {
            this.jobId = ExceptionsHelper.requireNonNull(jobId, "jobId");
            this.categoryId = ExceptionsHelper.requireNonNull(categoryId, "categoryId");
        }

        private Request() {
        }

        @Override
        public ActionRequestValidationException validate() {
            return null;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            jobId = in.readString();
            categoryId = in.readString();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeString(jobId);
            out.writeString(categoryId);
        }
    }

    public static class RequestBuilder extends ActionRequestBuilder<Request, Response, RequestBuilder> {

        public RequestBuilder(ElasticsearchClient client, GetCategoryDefinitionAction action) {
            super(client, action, new Request());
        }
    }

    public static class Response extends ActionResponse  implements StatusToXContent {

        private SingleDocument result;

        public Response(SingleDocument result) {
            this.result = result;
        }

        private Response() {
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
            result = new SingleDocument(in.readString(), in.readOptionalBytesReference());
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeString(result.getType());
            out.writeOptionalBytesReference(result.getDocumentBytes());
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
            Optional<CategoryDefinition> result = jobProvider.categoryDefinition(request.jobId, request.categoryId);
            try {
                SingleDocument singleDocument;
                if (result.isPresent()) {
                    BytesReference document = new BytesArray(objectMapper.writeValueAsBytes(result.get()));
                    singleDocument = new SingleDocument(CategoryDefinition.TYPE.getPreferredName(), document);
                } else {
                    singleDocument = SingleDocument.empty(CategoryDefinition.TYPE.getPreferredName());
                }
                listener.onResponse(new Response(singleDocument));
            } catch (JsonProcessingException e) {
                listener.onFailure(e);
            }
        }
    }
}
