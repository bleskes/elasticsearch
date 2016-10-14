package org.elasticsearch.xpack.prelert.rest.data;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.AcknowledgedRestListener;
import org.elasticsearch.xpack.prelert.action.PostDataFlushAction;

import java.io.IOException;

public class RestPostDataFlushAction extends BaseRestHandler {

    private final PostDataFlushAction.TransportAction transportPostDataFlushAction;

    @Inject
    public RestPostDataFlushAction(Settings settings, RestController controller,
                                   PostDataFlushAction.TransportAction transportPostDataFlushAction) {
        super(settings);
        this.transportPostDataFlushAction = transportPostDataFlushAction;
        controller.registerHandler(RestRequest.Method.POST, "/engine/v2/data/{jobId}/flush", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        PostDataFlushAction.Request postDataFlushRequest = new PostDataFlushAction.Request();
        return channel -> transportPostDataFlushAction.execute(postDataFlushRequest, new AcknowledgedRestListener<>(channel));
    }
}
