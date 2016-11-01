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
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.AcknowledgedRestListener;
import org.elasticsearch.rest.action.RestActions;
import org.elasticsearch.xpack.prelert.action.CreateListAction;

import java.io.IOException;

public class RestCreateListAction extends BaseRestHandler {

    private final CreateListAction.TransportAction transportCreateListAction;

    @Inject
    public RestCreateListAction(Settings settings, RestController controller, CreateListAction.TransportAction transportCreateListAction) {
        super(settings);
        this.transportCreateListAction = transportCreateListAction;
        controller.registerHandler(RestRequest.Method.POST, "/engine/v2/lists", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        BytesReference bodyBytes = RestActions.getRestContent(restRequest);
        XContentParser parser = XContentFactory.xContent(bodyBytes).createParser(bodyBytes);
        CreateListAction.Request createListRequest = CreateListAction.Request.parseRequest(parser, () -> parseFieldMatcher);
        return channel -> transportCreateListAction.execute(createListRequest, new AcknowledgedRestListener<>(channel));
    }

}
