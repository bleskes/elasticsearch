/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */
package org.elasticsearch.xpack.prelert.rest.buckets;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.action.RestBuilderListener;
import org.elasticsearch.rest.action.RestStatusToXContentListener;
import org.elasticsearch.xpack.prelert.action.GetBucketAction;

import static org.elasticsearch.rest.RestStatus.OK;

public class RestGetBucketAction extends BaseRestHandler {

    private final GetBucketAction.TransportAction transportAction;

    @Inject
    public RestGetBucketAction(Settings settings, RestController controller, GetBucketAction.TransportAction transportAction) {
        super(settings);
        this.transportAction = transportAction;
        controller.registerHandler(RestRequest.Method.GET, "/engine/v2/results/{jobId}/bucket/{timestamp}", this);
    }

    @Override
    public void handleRequest(RestRequest restRequest, RestChannel channel, NodeClient client) throws Exception {
        GetBucketAction.Request request = new GetBucketAction.Request(restRequest.param("jobId"), restRequest.param("timestamp"));
        request.setExpand(restRequest.paramAsBoolean("expand", false));
        request.setIncludeInterim(restRequest.paramAsBoolean("includeInterim", false));
        if (restRequest.hasParam("partitionValue")) {
            request.setPartitionValue(restRequest.param("partitionValue"));
        }
        transportAction.execute(request, new RestStatusToXContentListener<>(channel));
    }
}
