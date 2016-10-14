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
package org.elasticsearch.xpack.prelert.rest.job;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.*;
import org.elasticsearch.rest.action.RestBuilderListener;
import org.elasticsearch.xpack.prelert.action.GetJobsAction;

import java.io.IOException;

import static org.elasticsearch.rest.RestStatus.OK;

public class RestGetJobsAction extends BaseRestHandler {

    private static final String SKIP = "skip";
    private static final String TAKE = "take";
    private static final int DEFAULT_SKIP = 0;
    private static final int DEFAULT_TAKE = 100;

    private final GetJobsAction.TransportAction transportGetJobsAction;

    @Inject
    public RestGetJobsAction(Settings settings, RestController controller, GetJobsAction.TransportAction transportGetJobsAction) {
        super(settings);
        this.transportGetJobsAction = transportGetJobsAction;
        controller.registerHandler(RestRequest.Method.GET, "/engine/v2/jobs", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        GetJobsAction.Request getJobsRequest = new GetJobsAction.Request();
        getJobsRequest.setPagination(restRequest.paramAsInt(SKIP, DEFAULT_SKIP), restRequest.paramAsInt(TAKE, DEFAULT_TAKE));
        return channel -> transportGetJobsAction.execute(getJobsRequest, new RestBuilderListener<GetJobsAction.Response>(channel) {

            @Override
            public RestResponse buildResponse(GetJobsAction.Response response, XContentBuilder builder) throws Exception {
                return new BytesRestResponse(OK, XContentType.JSON.mediaType(), response.getResponse());
            }
        });
    }
}
