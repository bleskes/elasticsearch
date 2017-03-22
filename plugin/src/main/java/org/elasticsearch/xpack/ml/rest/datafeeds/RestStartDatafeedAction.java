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
package org.elasticsearch.xpack.ml.rest.datafeeds;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.rest.action.RestBuilderListener;
import org.elasticsearch.xpack.ml.MachineLearning;
import org.elasticsearch.xpack.ml.action.StartDatafeedAction;
import org.elasticsearch.xpack.ml.datafeed.DatafeedConfig;

import java.io.IOException;

public class RestStartDatafeedAction extends BaseRestHandler {

    private static final String DEFAULT_START = "0";

    public RestStartDatafeedAction(Settings settings, RestController controller) {
        super(settings);
        controller.registerHandler(RestRequest.Method.POST,
                MachineLearning.BASE_PATH + "datafeeds/{" + DatafeedConfig.ID.getPreferredName() + "}/_start", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        String datafeedId = restRequest.param(DatafeedConfig.ID.getPreferredName());
        StartDatafeedAction.Request jobDatafeedRequest;
        if (restRequest.hasContentOrSourceParam()) {
            XContentParser parser = restRequest.contentOrSourceParamParser();
            jobDatafeedRequest = StartDatafeedAction.Request.parseRequest(datafeedId, parser);
        } else {
            String startTime = restRequest.param(StartDatafeedAction.START_TIME.getPreferredName(), DEFAULT_START);
            jobDatafeedRequest = new StartDatafeedAction.Request(datafeedId, startTime);
            if (restRequest.hasParam(StartDatafeedAction.END_TIME.getPreferredName())) {
                jobDatafeedRequest.setEndTime(restRequest.param(StartDatafeedAction.END_TIME.getPreferredName()));
            }
            if (restRequest.hasParam(StartDatafeedAction.TIMEOUT.getPreferredName())) {
                TimeValue openTimeout = restRequest.paramAsTime(
                        StartDatafeedAction.TIMEOUT.getPreferredName(), TimeValue.timeValueSeconds(20));
                jobDatafeedRequest.setTimeout(openTimeout);
            }
        }
        return channel -> {
            client.execute(StartDatafeedAction.INSTANCE, jobDatafeedRequest,
                    new RestBuilderListener<StartDatafeedAction.Response>(channel) {

                        @Override
                        public RestResponse buildResponse(StartDatafeedAction.Response r, XContentBuilder builder) throws Exception {
                            builder.startObject();
                            builder.field("started", r.isAcknowledged());
                            builder.endObject();
                            return new BytesRestResponse(RestStatus.OK, builder);
                        }
                    });
        };
    }
}
