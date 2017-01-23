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
package org.elasticsearch.xpack.ml.rest.job;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestToXContentListener;
import org.elasticsearch.xpack.ml.MlPlugin;
import org.elasticsearch.xpack.ml.action.GetJobsAction;
import org.elasticsearch.xpack.ml.job.Job;

import java.io.IOException;

public class RestGetJobsAction extends BaseRestHandler {

    public RestGetJobsAction(Settings settings, RestController controller) {
        super(settings);

        controller.registerHandler(RestRequest.Method.GET, MlPlugin.BASE_PATH
                + "anomaly_detectors/{" + Job.ID.getPreferredName() + "}", this);
        controller.registerHandler(RestRequest.Method.GET, MlPlugin.BASE_PATH
                + "anomaly_detectors", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        String jobId = restRequest.param(Job.ID.getPreferredName());
        if (Strings.isNullOrEmpty(jobId)) {
            jobId = Job.ALL;
        }
        GetJobsAction.Request request = new GetJobsAction.Request(jobId);
        return channel -> client.execute(GetJobsAction.INSTANCE, request, new RestToXContentListener<>(channel));
    }
}
