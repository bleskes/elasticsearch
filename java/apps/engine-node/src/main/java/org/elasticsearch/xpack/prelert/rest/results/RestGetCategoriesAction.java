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

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.*;
import org.elasticsearch.rest.action.RestBuilderListener;
import org.elasticsearch.xpack.prelert.action.GetCategoryDefinitionsAction;

import java.io.IOException;

import static org.elasticsearch.rest.RestStatus.OK;

public class RestGetCategoriesAction extends BaseRestHandler {

    private final GetCategoryDefinitionsAction.TransportAction transportAction;

    @Inject
    public RestGetCategoriesAction(Settings settings, RestController controller, GetCategoryDefinitionsAction.TransportAction transportAction) {
        super(settings);
        this.transportAction = transportAction;
        controller.registerHandler(RestRequest.Method.GET, "/engine/v2/results/{jobId}/categorydefinitions", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        GetCategoryDefinitionsAction.Request request = new GetCategoryDefinitionsAction.Request(restRequest.param("jobId"));
        request.setPagination(restRequest.paramAsInt("skip", 0), restRequest.paramAsInt("take", 100));
        return channel -> transportAction.execute(request, new RestBuilderListener<GetCategoryDefinitionsAction.Response>(channel) {

            @Override
            public RestResponse buildResponse(GetCategoryDefinitionsAction.Response response, XContentBuilder builder) throws Exception {
                return new BytesRestResponse(OK, XContentType.JSON.mediaType(), response.getResponse());
            }
        });
    }

}
