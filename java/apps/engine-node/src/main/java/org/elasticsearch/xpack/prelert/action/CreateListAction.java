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

import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.action.Action;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.index.TransportIndexAction;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.support.master.MasterNodeOperationRequestBuilder;
import org.elasticsearch.action.support.master.MasterNodeReadRequest;
import org.elasticsearch.action.support.master.TransportMasterNodeAction;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.ParseFieldMatcherSupplier;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.prelert.lists.ListDocument;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

import java.io.IOException;
import java.util.Objects;


public class CreateListAction extends Action<CreateListAction.Request, CreateListAction.Response, CreateListAction.RequestBuilder> {

    public static final CreateListAction INSTANCE = new CreateListAction();
    public static final String NAME = "cluster:admin/prelert/list/create";

    private CreateListAction() {
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

    public static class Request extends MasterNodeReadRequest<Request> implements ToXContent {

        public static Request parseRequest(XContentParser parser, ParseFieldMatcherSupplier matcherSupplier) {
            ListDocument listDocument = ListDocument.PARSER.apply(parser, matcherSupplier);
            return new Request(listDocument);
        }

        private ListDocument listDocument;

        Request() {

        }

        public Request(ListDocument listDocument) {
            this.listDocument = ExceptionsHelper.requireNonNull(listDocument, "listDocument");
        }

        public ListDocument getListDocument() {
            return this.listDocument;
        }

        @Override
        public ActionRequestValidationException validate() {
            return null;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            listDocument = new ListDocument(in);
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            listDocument.writeTo(out);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            listDocument.toXContent(builder, params);
            return builder;
        }

        @Override
        public int hashCode() {
            return Objects.hash(listDocument);
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
            return Objects.equals(listDocument, other.listDocument);
        }
    }

    public static class RequestBuilder extends MasterNodeOperationRequestBuilder<Request, Response, RequestBuilder> {

        public RequestBuilder(ElasticsearchClient client, CreateListAction action) {
            super(client, action, new Request());
        }
    }
    public static class Response extends AcknowledgedResponse {

        public Response() {
            super(true);
        }

    }

    // extends TransportMasterNodeAction, because we will store in cluster state.
    public static class TransportAction extends TransportMasterNodeAction<Request, Response> {

        private final TransportIndexAction transportIndexAction;

        // TODO these need to be moved to a settings object later. See #20
        private static final String PRELERT_INFO_INDEX = "prelert-int";

        @Inject
        public TransportAction(Settings settings, TransportService transportService, ClusterService clusterService,
                ThreadPool threadPool, ActionFilters actionFilters,
                IndexNameExpressionResolver indexNameExpressionResolver,
                TransportIndexAction transportIndexAction) {
            super(settings, CreateListAction.NAME, transportService, clusterService, threadPool, actionFilters,
                    indexNameExpressionResolver, Request::new);
            this.transportIndexAction = transportIndexAction;
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
            ListDocument listDocument = request.getListDocument();
            final String listId = listDocument.getId();
            IndexRequest indexRequest = new IndexRequest(PRELERT_INFO_INDEX, ListDocument.TYPE.getPreferredName(), listId);
            XContentBuilder builder = XContentFactory.jsonBuilder();
            indexRequest.source(listDocument.toXContent(builder, ToXContent.EMPTY_PARAMS));
            transportIndexAction.execute(indexRequest, new ActionListener<IndexResponse>() {
                @Override
                public void onResponse(IndexResponse indexResponse) {
                    listener.onResponse(new Response());
                }

                @Override
                public void onFailure(Exception e) {
                    logger.error("Could not create list with ID [" + listId + "]", e);
                    throw new ResourceNotFoundException("Could not create list with ID [" + listId + "]", e);
                }
            });
        }

        @Override
        protected ClusterBlockException checkBlock(Request request, ClusterState state) {
            return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_WRITE);
        }
    }
}

