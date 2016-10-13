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
package org.elasticsearch.xpack.prelert.rest.influencers;

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
import org.elasticsearch.xpack.prelert.action.GetInfluencersAction;
import org.elasticsearch.xpack.prelert.job.results.Influencer;

import static org.elasticsearch.rest.RestStatus.OK;

public class RestGetInfluencersAction extends BaseRestHandler {

    private final GetInfluencersAction.TransportAction transportAction;

    @Inject
    public RestGetInfluencersAction(Settings settings, RestController controller, GetInfluencersAction.TransportAction transportAction) {
        super(settings);
        this.transportAction = transportAction;
        controller.registerHandler(RestRequest.Method.GET, "/engine/v2/results/{jobId}/influencers", this);
    }

    @Override
    public void handleRequest(RestRequest restRequest, RestChannel channel, NodeClient client) throws Exception {
        GetInfluencersAction.Request request = new GetInfluencersAction.Request(restRequest.param("jobId"), restRequest.param("start"),
                restRequest.param("end"));
        request.setIncludeInterim(restRequest.paramAsBoolean("includeInterim", false));
        request.setPagination(restRequest.paramAsInt("skip", 0), restRequest.paramAsInt("take", 100));
        request.setAnomalyScore(Double.parseDouble(restRequest.param("anomalyScore", "0.0")));
        request.setSort(restRequest.param("sort", Influencer.ANOMALY_SCORE));
        request.setDecending(restRequest.paramAsBoolean("desc", false));

        transportAction.execute(request, new RestBuilderListener<GetInfluencersAction.Response>(channel) {

            @Override
            public RestResponse buildResponse(GetInfluencersAction.Response response, XContentBuilder builder) throws Exception {
                return new BytesRestResponse(OK, XContentType.JSON.mediaType(), response.getResponse());
            }
        });
    }
}
