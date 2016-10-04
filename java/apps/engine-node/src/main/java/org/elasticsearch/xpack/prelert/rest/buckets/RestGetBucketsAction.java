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
import org.elasticsearch.xpack.prelert.action.GetBucketsAction;

import static org.elasticsearch.rest.RestStatus.OK;

public class RestGetBucketsAction extends BaseRestHandler {

    private final GetBucketsAction.TransportAction transportAction;

    @Inject
    public RestGetBucketsAction(Settings settings, RestController controller, GetBucketsAction.TransportAction transportAction) {
        super(settings);
        this.transportAction = transportAction;
        controller.registerHandler(RestRequest.Method.GET, "/engine/v2/results/{jobId}/buckets", this);
    }

    @Override
    public void handleRequest(RestRequest restRequest, RestChannel channel, NodeClient client) throws Exception {
        GetBucketsAction.Request request =
                new GetBucketsAction.Request(restRequest.param("jobId"), restRequest.param("start"), restRequest.param("end"));
        request.setExpand(restRequest.paramAsBoolean("expand", false));
        request.setIncludeInterim(restRequest.paramAsBoolean("includeInterim", false));
        request.setSkip(restRequest.paramAsInt("skip", 0));
        request.setTake(restRequest.paramAsInt("take", 100));
        request.setAnomalyScore(Double.parseDouble(restRequest.param("anomalyScore", "0.0")));
        request.setMaxNormalizedProbability(Double.parseDouble(restRequest.param("maxNormalizedProbability", "0.0")));
        if (restRequest.hasParam("partitionValue")) {
            request.setPartitionValue(restRequest.param("partitionValue"));
        }

        transportAction.execute(request, new RestBuilderListener<GetBucketsAction.Response>(channel) {

            @Override
            public RestResponse buildResponse(GetBucketsAction.Response response, XContentBuilder builder) throws Exception {
                return new BytesRestResponse(OK, XContentType.JSON.mediaType(), response.getResponse());
            }
        });
    }
}
