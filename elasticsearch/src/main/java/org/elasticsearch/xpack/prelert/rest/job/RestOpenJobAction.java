/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
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
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.AcknowledgedRestListener;
import org.elasticsearch.xpack.prelert.PrelertPlugin;
import org.elasticsearch.xpack.prelert.action.JobDataAction;
import org.elasticsearch.xpack.prelert.action.OpenJobAction;
import org.elasticsearch.xpack.prelert.job.Job;

import java.io.IOException;

public class RestOpenJobAction extends BaseRestHandler {

    private final OpenJobAction.TransportAction openJobAction;

    @Inject
    public RestOpenJobAction(Settings settings, RestController controller, OpenJobAction.TransportAction openJobAction) {
        super(settings);
        this.openJobAction = openJobAction;
        controller.registerHandler(RestRequest.Method.POST, PrelertPlugin.BASE_PATH
                + "anomaly_detectors/{" + Job.ID.getPreferredName() + "}/_open", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        OpenJobAction.Request request = new OpenJobAction.Request(restRequest.param(Job.ID.getPreferredName()));
        request.setIgnoreDowntime(restRequest.paramAsBoolean(JobDataAction.Request.IGNORE_DOWNTIME.getPreferredName(), false));
        if (restRequest.hasParam(OpenJobAction.Request.OPEN_TIMEOUT.getPreferredName())) {
            request.setOpenTimeout(TimeValue.parseTimeValue(
                    restRequest.param(OpenJobAction.Request.OPEN_TIMEOUT.getPreferredName()),
                    OpenJobAction.Request.OPEN_TIMEOUT.getPreferredName()));
        }
        return channel -> openJobAction.execute(request, new AcknowledgedRestListener<>(channel));
    }
}
