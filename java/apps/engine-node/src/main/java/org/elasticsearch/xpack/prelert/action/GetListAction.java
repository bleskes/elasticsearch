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
import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.action.Action;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.TransportGetAction;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.master.MasterNodeReadOperationRequestBuilder;
import org.elasticsearch.action.support.master.MasterNodeReadRequest;
import org.elasticsearch.action.support.master.TransportMasterNodeReadAction;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.StatusToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.prelert.lists.ListDocument;
import org.elasticsearch.xpack.prelert.utils.SingleDocument;

import java.io.IOException;

import static org.elasticsearch.action.ValidateActions.addValidationError;


public class GetListAction extends Action<GetListAction.Request, GetListAction.Response, GetListAction.RequestBuilder> {

    public static final GetListAction INSTANCE = new GetListAction();
    public static final String NAME = "cluster:admin/prelert/list/get";

    private GetListAction() {
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

    public static class Request extends MasterNodeReadRequest<Request> {

        private String listId;

        public String getListId() {
            return listId;
        }

        public void setListId(String listId) {
            this.listId = listId;
        }

        @Override
        public ActionRequestValidationException validate() {
            ActionRequestValidationException validationException = null;
            if (listId == null) {
                validationException = addValidationError("List ID is required for GetList API.", validationException);
            }
            return validationException;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            listId = in.readOptionalString();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeOptionalString(listId);
        }
    }

    public static class RequestBuilder extends MasterNodeReadOperationRequestBuilder<Request, Response, RequestBuilder> {

        public RequestBuilder(ElasticsearchClient client, GetListAction action) {
            super(client, action, new Request());
        }
    }

    public static class Response extends ActionResponse implements StatusToXContent {

        private SingleDocument response;

        public Response(SingleDocument document) throws JsonProcessingException {
            this.response = document;
        }

        public Response() {
        }

        public SingleDocument getResponse() {
            return response;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            response = new SingleDocument(in.readString(), in.readBytesReference());
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeString(response.getType());
            out.writeBytesReference(response.getDocumentBytes());
        }

        @Override
        public RestStatus status() {
            return response.status();
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            return response.toXContent(builder, params);
        }
    }

    public static class TransportAction extends TransportMasterNodeReadAction<Request, Response> {

        private final ObjectMapper objectMapper = new ObjectMapper();
        private final TransportGetAction transportGetAction;

        // TODO these need to be moved to a settings object later
        // See #20
        private static final String PRELERT_INFO_INDEX = "prelert-int";

        @Inject
        public TransportAction(Settings settings, TransportService transportService, ClusterService clusterService,
                ThreadPool threadPool, ActionFilters actionFilters,
                IndexNameExpressionResolver indexNameExpressionResolver,
                TransportGetAction transportGetAction) {
            super(settings, GetListAction.NAME, transportService, clusterService, threadPool, actionFilters,
                    indexNameExpressionResolver, Request::new);
            this.transportGetAction = transportGetAction;
        }

        @Override
        protected String executor() {
            return ThreadPool.Names.SAME;
        }

        @Override
        protected Response newResponse() {
            return new Response();
        }

        @Override
        protected void masterOperation(Request request, ClusterState state, ActionListener<Response> listener) throws Exception {
            final String listId = request.getListId();
            GetRequest getRequest = new GetRequest(PRELERT_INFO_INDEX, ListDocument.TYPE.getPreferredName(), listId);
            transportGetAction.execute(getRequest, new ActionListener<GetResponse>() {
                @Override
                public void onResponse(GetResponse getDocResponse) {
                    SingleDocument responseBody;
                    if (getDocResponse.isExists()) {
                        responseBody = new SingleDocument(ListDocument.TYPE.getPreferredName(), getDocResponse.getSourceAsBytesRef());
                    } else {
                        responseBody = SingleDocument.empty(ListDocument.TYPE.getPreferredName());
                    }

                    try {
                        Response listResponse = new Response(responseBody);
                        listener.onResponse(listResponse);
                    } catch (Exception e) {
                        this.onFailure(e);
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    throw new ResourceNotFoundException("List with id [" + listId + "] not found", e);
                }
            });
        }

        @Override
        protected ClusterBlockException checkBlock(Request request, ClusterState state) {
            return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_READ);
        }
    }

}

