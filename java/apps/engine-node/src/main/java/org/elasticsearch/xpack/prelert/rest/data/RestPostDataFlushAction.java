package org.elasticsearch.xpack.prelert.rest.data;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.*;
import org.elasticsearch.rest.action.AcknowledgedRestListener;
import org.elasticsearch.xpack.prelert.action.PostDataFlushAction;

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
    public void handleRequest(RestRequest request, RestChannel channel, NodeClient client) throws Exception {
        PostDataFlushAction.Request postDataFlushRequest = new PostDataFlushAction.Request();
        transportPostDataFlushAction.execute(postDataFlushRequest, new AcknowledgedRestListener<>(channel));
    }
}
