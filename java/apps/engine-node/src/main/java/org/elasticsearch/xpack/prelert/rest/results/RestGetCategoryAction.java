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
package org.elasticsearch.xpack.prelert.rest.results;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestStatusToXContentListener;
import org.elasticsearch.xpack.prelert.PrelertPlugin;
import org.elasticsearch.xpack.prelert.action.GetCategoryDefinitionAction;

import java.io.IOException;

public class RestGetCategoryAction extends BaseRestHandler {

    private final GetCategoryDefinitionAction.TransportAction transportAction;

    @Inject
    public RestGetCategoryAction(Settings settings, RestController controller,
            GetCategoryDefinitionAction.TransportAction transportAction) {
        super(settings);
        this.transportAction = transportAction;
        controller.registerHandler(RestRequest.Method.GET, PrelertPlugin.BASE_PATH + "results/{jobId}/categorydefinitions/{categoryId}",
                this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        GetCategoryDefinitionAction.Request request = new GetCategoryDefinitionAction.Request(restRequest.param("jobId"),
                restRequest.param("categoryId"));
        return channel -> transportAction.execute(request, new RestStatusToXContentListener<>(channel));
    }

}
