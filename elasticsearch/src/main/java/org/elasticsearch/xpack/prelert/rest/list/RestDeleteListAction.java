/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.prelert.rest.list;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.AcknowledgedRestListener;
import org.elasticsearch.xpack.prelert.PrelertPlugin;
import org.elasticsearch.xpack.prelert.action.DeleteListAction;
import org.elasticsearch.xpack.prelert.action.DeleteListAction.Request;

import java.io.IOException;

public class RestDeleteListAction extends BaseRestHandler {

    private final DeleteListAction.TransportAction transportAction;

    @Inject
    public RestDeleteListAction(Settings settings, RestController controller, DeleteListAction.TransportAction transportAction) {
        super(settings);
        this.transportAction = transportAction;
        controller.registerHandler(RestRequest.Method.DELETE,
                PrelertPlugin.BASE_PATH + "lists/{" + Request.LIST_ID.getPreferredName() + "}", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        Request request = new Request(restRequest.param(Request.LIST_ID.getPreferredName()));
        return channel -> transportAction.execute(request, new AcknowledgedRestListener<>(channel));
    }

}
