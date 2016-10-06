package org.elasticsearch.xpack.prelert.action;

import org.elasticsearch.action.Action;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.support.master.MasterNodeOperationRequestBuilder;
import org.elasticsearch.action.support.master.MasterNodeRequest;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.prelert.PrelertServices;

public class PostDataFlushAction extends Action<PostDataFlushAction.Request, PostDataFlushAction.Response,
        PostDataFlushAction.RequestBuilder> {

    public static final PostDataFlushAction INSTANCE = new PostDataFlushAction();
    public static final String NAME = "cluster:admin/prelert/data/post/flush";

    private PostDataFlushAction() {
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

    public static class Request extends MasterNodeRequest<Request> {

        @Override
        public ActionRequestValidationException validate() {
            return null;
        }
    }

    static class RequestBuilder extends MasterNodeOperationRequestBuilder<Request, Response, RequestBuilder> {

        public RequestBuilder(ElasticsearchClient client, PostDataFlushAction action) {
            super(client, action, new Request());
        }
    }

    public static class Response extends AcknowledgedResponse {

        public Response() {
        }
    }

    public static class TransportAction extends HandledTransportAction<Request, Response> {

        @Inject
        public TransportAction(Settings settings, TransportService transportService, ClusterService clusterService,
                               ThreadPool threadPool, ActionFilters actionFilters,
                               IndexNameExpressionResolver indexNameExpressionResolver, PrelertServices prelertServices) {
            super(settings, PostDataFlushAction.NAME, false, threadPool, transportService, actionFilters,
                    indexNameExpressionResolver, PostDataFlushAction.Request::new);
        }

        @Override
        protected final void doExecute(PostDataFlushAction.Request request, ActionListener<PostDataFlushAction.Response> listener) {
            String msg = "Post data flush action not implemented";
            this.logger.warn(msg);
            throw new UnsupportedOperationException(msg);
        }
    }
}
 

