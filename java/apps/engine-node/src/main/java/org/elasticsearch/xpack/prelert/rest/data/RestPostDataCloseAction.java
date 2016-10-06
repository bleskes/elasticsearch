package org.elasticsearch.xpack.prelert.rest.data;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.AcknowledgedRestListener;
import org.elasticsearch.xpack.prelert.action.PostDataCloseAction;


public class RestPostDataCloseAction extends BaseRestHandler {

    private final PostDataCloseAction.TransportAction transportPostDataCloseAction;

    @Inject
    public RestPostDataCloseAction(Settings settings, RestController controller, PostDataCloseAction.TransportAction transportPostDataCloseAction) {
        super(settings);
        this.transportPostDataCloseAction = transportPostDataCloseAction;
        controller.registerHandler(RestRequest.Method.POST, "/engine/v2/data/{jobId}/close", this);
    }

    @Override
    public void handleRequest(RestRequest request, RestChannel channel, NodeClient client) throws Exception {
        PostDataCloseAction.Request postDataCloseRequest = new PostDataCloseAction.Request();

        transportPostDataCloseAction.execute(postDataCloseRequest, new AcknowledgedRestListener<>(channel));
    }
}

