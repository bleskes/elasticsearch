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
package org.elasticsearch.xpack.prelert.rest.job;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestStatusToXContentListener;
import org.elasticsearch.xpack.prelert.PrelertPlugin;
import org.elasticsearch.xpack.prelert.action.GetJobsStatsAction;
import org.elasticsearch.xpack.prelert.job.Job;

import java.io.IOException;

public class RestGetJobsStatsAction extends BaseRestHandler {

    private final GetJobsStatsAction.TransportAction transportGetJobsStatsAction;

    @Inject
    public RestGetJobsStatsAction(Settings settings, RestController controller,
                                  GetJobsStatsAction.TransportAction transportGetJobsStatsAction) {
        super(settings);
        this.transportGetJobsStatsAction = transportGetJobsStatsAction;

        controller.registerHandler(RestRequest.Method.GET, PrelertPlugin.BASE_PATH
                + "anomaly_detectors/{" + Job.ID.getPreferredName() + "}/_stats", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        GetJobsStatsAction.Request request = new GetJobsStatsAction.Request(restRequest.param(Job.ID.getPreferredName()));
        return channel -> transportGetJobsStatsAction.execute(request, new RestStatusToXContentListener<>(channel));
    }
}
