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
package org.elasticsearch.xpack.prelert.rest.schedulers;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestStatusToXContentListener;
import org.elasticsearch.xpack.prelert.PrelertPlugin;
import org.elasticsearch.xpack.prelert.action.GetSchedulersAction;
import org.elasticsearch.xpack.prelert.scheduler.SchedulerConfig;

import java.io.IOException;

public class RestGetSchedulersAction extends BaseRestHandler {

    private final GetSchedulersAction.TransportAction transportGetSchedulersAction;

    @Inject
    public RestGetSchedulersAction(Settings settings, RestController controller,
                                   GetSchedulersAction.TransportAction transportGetSchedulersAction) {
        super(settings);
        this.transportGetSchedulersAction = transportGetSchedulersAction;

        controller.registerHandler(RestRequest.Method.GET, PrelertPlugin.BASE_PATH
                + "schedulers/{" + SchedulerConfig.ID.getPreferredName() + "}", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        GetSchedulersAction.Request request = new GetSchedulersAction.Request(restRequest.param(SchedulerConfig.ID.getPreferredName()));
        return channel -> transportGetSchedulersAction.execute(request, new RestStatusToXContentListener<>(channel));
    }
}
