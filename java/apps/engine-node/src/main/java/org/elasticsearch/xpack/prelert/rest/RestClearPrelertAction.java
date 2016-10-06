package org.elasticsearch.xpack.prelert.rest;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.AcknowledgedRestListener;
import org.elasticsearch.xpack.prelert.action.ClearPrelertAction;

public class RestClearPrelertAction extends BaseRestHandler {

    private final ClearPrelertAction.TransportAction transportAction;

    @Inject
    public RestClearPrelertAction(Settings settings, RestController controller, ClearPrelertAction.TransportAction transportAction) {
        super(settings);
        this.transportAction = transportAction;
        controller.registerHandler(RestRequest.Method.DELETE, "/engine/v2/clear", this);
    }

    @Override
    public void handleRequest(RestRequest request, RestChannel channel, NodeClient client) throws Exception {
        transportAction.execute(new ClearPrelertAction.Request(), new AcknowledgedRestListener<>(channel));
    }

}
