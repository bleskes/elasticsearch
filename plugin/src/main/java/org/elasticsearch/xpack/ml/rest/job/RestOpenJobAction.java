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
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.rest.action.RestBuilderListener;
import org.elasticsearch.xpack.ml.MachineLearning;
import org.elasticsearch.xpack.ml.action.OpenJobAction;
import org.elasticsearch.xpack.ml.job.config.Job;

import java.io.IOException;

public class RestOpenJobAction extends BaseRestHandler {

    public RestOpenJobAction(Settings settings, RestController controller) {
        super(settings);
        controller.registerHandler(RestRequest.Method.POST, MachineLearning.BASE_PATH
                + "anomaly_detectors/{" + Job.ID.getPreferredName() + "}/_open", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        OpenJobAction.Request request;
        if (restRequest.hasContentOrSourceParam()) {
            request = OpenJobAction.Request.parseRequest(restRequest.param(Job.ID.getPreferredName()), restRequest.contentParser());
        } else {
            OpenJobAction.JobParams jobParams = new OpenJobAction.JobParams(restRequest.param(Job.ID.getPreferredName()));
            jobParams.setIgnoreDowntime(restRequest.paramAsBoolean(OpenJobAction.JobParams.IGNORE_DOWNTIME.getPreferredName(), true));
            if (restRequest.hasParam(OpenJobAction.JobParams.TIMEOUT.getPreferredName())) {
                TimeValue openTimeout = restRequest.paramAsTime(OpenJobAction.JobParams.TIMEOUT.getPreferredName(),
                        TimeValue.timeValueSeconds(20));
                jobParams.setTimeout(openTimeout);
            }
            request = new OpenJobAction.Request(jobParams);
        }
        return channel -> {
            client.execute(OpenJobAction.INSTANCE, request, new RestBuilderListener<OpenJobAction.Response>(channel) {
                @Override
                public RestResponse buildResponse(OpenJobAction.Response r, XContentBuilder builder) throws Exception {
                    builder.startObject();
                    builder.field("opened", r.isAcknowledged());
                    builder.endObject();
                    return new BytesRestResponse(RestStatus.OK, builder);
                }
            });
        };
    }
}
