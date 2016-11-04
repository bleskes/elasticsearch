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
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestToXContentListener;
import org.elasticsearch.xpack.prelert.action.GetBucketsAction;
import org.elasticsearch.xpack.prelert.action.GetBucketsAction.Response;
import org.elasticsearch.xpack.prelert.job.Job;
import org.elasticsearch.xpack.prelert.job.results.PageParams;

import java.io.IOException;

public class RestGetBucketsAction extends BaseRestHandler {

    private final GetBucketsAction.TransportAction transportAction;

    @Inject
    public RestGetBucketsAction(Settings settings, RestController controller, GetBucketsAction.TransportAction transportAction) {
        super(settings);
        this.transportAction = transportAction;
        controller.registerHandler(RestRequest.Method.GET, "/engine/v2/results/{" + Job.ID.getPreferredName() + "}/buckets", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        String jobId = restRequest.param(Job.ID.getPreferredName());
        BytesReference bodyBytes = restRequest.content();
        final GetBucketsAction.Request request;
        if (bodyBytes != null && bodyBytes.length() > 0) {
            XContentParser parser = XContentFactory.xContent(bodyBytes).createParser(bodyBytes);
            request = GetBucketsAction.Request.parseRequest(jobId, parser, () -> parseFieldMatcher);
        } else {
            request = new GetBucketsAction.Request(jobId, restRequest.param(GetBucketsAction.Request.START.getPreferredName()),
                    restRequest.param(GetBucketsAction.Request.END.getPreferredName()));
            request.setExpand(restRequest.paramAsBoolean(GetBucketsAction.Request.EXPAND.getPreferredName(), false));
            request.setIncludeInterim(restRequest.paramAsBoolean(GetBucketsAction.Request.INCLUDE_INTERIM.getPreferredName(), false));
            request.setPageParams(new PageParams(restRequest.paramAsInt(PageParams.SKIP.getPreferredName(), 0),
                    restRequest.paramAsInt(PageParams.TAKE.getPreferredName(), 100)));
            request.setAnomalyScore(
                    Double.parseDouble(restRequest.param(GetBucketsAction.Request.ANOMALY_SCORE.getPreferredName(), "0.0")));
            request.setMaxNormalizedProbability(
                    Double.parseDouble(restRequest.param(GetBucketsAction.Request.MAX_NORMALIZED_PROBABILITY.getPreferredName(), "0.0")));
            if (restRequest.hasParam(GetBucketsAction.Request.PARTITION_VALUE.getPreferredName())) {
                request.setPartitionValue(restRequest.param(GetBucketsAction.Request.PARTITION_VALUE.getPreferredName()));
            }
        }

        return channel -> transportAction.execute(request, new RestToXContentListener<Response>(channel));
    }
}
