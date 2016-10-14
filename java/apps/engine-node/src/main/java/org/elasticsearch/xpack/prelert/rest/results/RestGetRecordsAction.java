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
package org.elasticsearch.xpack.prelert.rest.results;

import static org.elasticsearch.rest.RestStatus.OK;

import java.io.IOException;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.action.RestBuilderListener;
import org.elasticsearch.xpack.prelert.action.GetRecordsAction;
import org.elasticsearch.xpack.prelert.job.results.AnomalyRecord;

public class RestGetRecordsAction extends BaseRestHandler {

    private final GetRecordsAction.TransportAction transportAction;

    @Inject
    public RestGetRecordsAction(Settings settings, RestController controller, GetRecordsAction.TransportAction transportAction) {
        super(settings);
        this.transportAction = transportAction;
        controller.registerHandler(RestRequest.Method.GET, "/engine/v2/results/{jobId}/records", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        GetRecordsAction.Request request = new GetRecordsAction.Request(restRequest.param("jobId"), restRequest.param("start"),
                restRequest.param("end"));
        request.setIncludeInterim(restRequest.paramAsBoolean("includeInterim", false));
        request.setPagination(restRequest.paramAsInt("skip", 0), restRequest.paramAsInt("take", 100));
        request.setAnomalyScore(Double.parseDouble(restRequest.param("anomalyScore", "0.0")));
        request.setSort(restRequest.param("sort", AnomalyRecord.NORMALIZED_PROBABILITY));
        request.setDecending(restRequest.paramAsBoolean("desc", false));
        request.setMaxNormalizedProbability(Double.parseDouble(restRequest.param("normalizedProbability", "0.0")));
        String partitionValue = restRequest.param("partitionValue");
        if (partitionValue != null) {
            request.setPartitionValue(partitionValue);
        }

        return channel -> transportAction.execute(request, new RestBuilderListener<GetRecordsAction.Response>(channel) {

            @Override
            public RestResponse buildResponse(GetRecordsAction.Response response, XContentBuilder builder) throws Exception {
                return new BytesRestResponse(OK, XContentType.JSON.mediaType(), response.getResponse());
            }
        });
    }
}
