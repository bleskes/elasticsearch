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
import org.elasticsearch.rest.action.AcknowledgedRestListener;
import org.elasticsearch.xpack.prelert.PrelertPlugin;
import org.elasticsearch.xpack.prelert.action.PauseJobAction;
import org.elasticsearch.xpack.prelert.job.Job;

import java.io.IOException;

public class RestPauseJobAction extends BaseRestHandler {

    private final PauseJobAction.TransportAction transportPauseJobAction;

    @Inject
    public RestPauseJobAction(Settings settings, RestController controller, PauseJobAction.TransportAction transportPauseJobAction) {
        super(settings);
        this.transportPauseJobAction = transportPauseJobAction;
        controller.registerHandler(RestRequest.Method.POST, PrelertPlugin.BASE_PATH + "jobs/{" + Job.ID.getPreferredName() + "}/pause",
                this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        PauseJobAction.Request request = new PauseJobAction.Request(restRequest.param(Job.ID.getPreferredName()));
        return channel -> transportPauseJobAction.execute(request, new AcknowledgedRestListener<>(channel));
    }
}
