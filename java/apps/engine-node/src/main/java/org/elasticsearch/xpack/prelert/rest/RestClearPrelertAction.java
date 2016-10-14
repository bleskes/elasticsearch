package org.elasticsearch.xpack.prelert.rest;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.AcknowledgedRestListener;
import org.elasticsearch.xpack.prelert.action.ClearPrelertAction;

import java.io.IOException;

public class RestClearPrelertAction extends BaseRestHandler {

    private final ClearPrelertAction.TransportAction transportAction;

    @Inject
    public RestClearPrelertAction(Settings settings, RestController controller, ClearPrelertAction.TransportAction transportAction) {
        super(settings);
        this.transportAction = transportAction;
        controller.registerHandler(RestRequest.Method.DELETE, "/engine/v2/clear", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        return channel -> transportAction.execute(new ClearPrelertAction.Request(), new AcknowledgedRestListener<>(channel));
    }

}
