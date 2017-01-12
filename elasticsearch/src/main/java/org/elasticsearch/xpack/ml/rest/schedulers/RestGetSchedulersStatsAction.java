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
package org.elasticsearch.xpack.ml.rest.schedulers;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestToXContentListener;
import org.elasticsearch.xpack.ml.MlPlugin;
import org.elasticsearch.xpack.ml.action.GetSchedulersStatsAction;
import org.elasticsearch.xpack.ml.scheduler.SchedulerConfig;

import java.io.IOException;

public class RestGetSchedulersStatsAction extends BaseRestHandler {

    private final GetSchedulersStatsAction.TransportAction transportGetSchedulersStatsAction;

    @Inject
    public RestGetSchedulersStatsAction(Settings settings, RestController controller,
                                        GetSchedulersStatsAction.TransportAction transportGetSchedulersStatsAction) {
        super(settings);
        this.transportGetSchedulersStatsAction = transportGetSchedulersStatsAction;

        controller.registerHandler(RestRequest.Method.GET, MlPlugin.BASE_PATH
                + "schedulers/{" + SchedulerConfig.ID.getPreferredName() + "}/_stats", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        GetSchedulersStatsAction.Request request = new GetSchedulersStatsAction.Request(
                restRequest.param(SchedulerConfig.ID.getPreferredName()));
        return channel -> transportGetSchedulersStatsAction.execute(request, new RestToXContentListener<>(channel));
    }
}
