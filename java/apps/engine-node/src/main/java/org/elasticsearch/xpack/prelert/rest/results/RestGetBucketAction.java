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
import org.elasticsearch.rest.action.RestStatusToXContentListener;
import org.elasticsearch.xpack.prelert.action.GetBucketAction;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.results.Bucket;

import java.io.IOException;

public class RestGetBucketAction extends BaseRestHandler {

    private final GetBucketAction.TransportAction transportAction;

    @Inject
    public RestGetBucketAction(Settings settings, RestController controller, GetBucketAction.TransportAction transportAction) {
        super(settings);
        this.transportAction = transportAction;
        controller.registerHandler(RestRequest.Method.GET,
                "/engine/v2/results/{" + JobDetails.ID.getPreferredName() + "}/bucket/{" + Bucket.TIMESTAMP.getPreferredName() + "}", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        String jobId = restRequest.param(JobDetails.ID.getPreferredName());
        String timestamp = restRequest.param(Bucket.TIMESTAMP.getPreferredName());
        final GetBucketAction.Request request;
        if (jobId != null) {
            request = new GetBucketAction.Request(jobId, timestamp);
            request.setExpand(restRequest.paramAsBoolean(GetBucketAction.Request.EXPAND.getPreferredName(), false));
            request.setIncludeInterim(restRequest.paramAsBoolean(GetBucketAction.Request.INCLUDE_INTERIM.getPreferredName(), false));
            if (restRequest.hasParam(GetBucketAction.Request.PARTITION_VALUE.getPreferredName())) {
                request.setPartitionValue(restRequest.param(GetBucketAction.Request.PARTITION_VALUE.getPreferredName()));
            }
        } else {
            BytesReference bodyBytes = restRequest.content();
            XContentParser parser = XContentFactory.xContent(bodyBytes).createParser(bodyBytes);
            request = GetBucketAction.Request.parseRequest(jobId, timestamp, parser, () -> parseFieldMatcher);
        }
        return channel -> transportAction.execute(request, new RestStatusToXContentListener<>(channel));
    }
}
