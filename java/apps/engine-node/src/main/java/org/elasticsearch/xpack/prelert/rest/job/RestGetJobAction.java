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
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestStatusToXContentListener;
import org.elasticsearch.xpack.prelert.action.GetJobAction;

import java.io.IOException;

public class RestGetJobAction extends BaseRestHandler {

    private static final String JOB_ID = "jobId";

    private final GetJobAction.TransportAction transportGetJobAction;

    @Inject
    public RestGetJobAction(Settings settings, RestController controller, GetJobAction.TransportAction transportGetJobAction) {
        super(settings);
        this.transportGetJobAction = transportGetJobAction;
        controller.registerHandler(RestRequest.Method.GET, "/engine/v2/jobs/{jobId}", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        GetJobAction.Request getJobRequest = new GetJobAction.Request();
        getJobRequest.setJobId(restRequest.param(JOB_ID));
        return channel -> transportGetJobAction.execute(getJobRequest, new RestStatusToXContentListener<>(channel));
    }
}
