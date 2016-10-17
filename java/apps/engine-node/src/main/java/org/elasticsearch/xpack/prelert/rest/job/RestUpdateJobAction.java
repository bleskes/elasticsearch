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
import org.elasticsearch.rest.action.AcknowledgedRestListener;
import org.elasticsearch.rest.action.RestActions;
import org.elasticsearch.xpack.prelert.action.UpdateJobAction;

import java.io.IOException;

public class RestUpdateJobAction extends BaseRestHandler {

    private static final String JOB_ID = "jobId";

    private final UpdateJobAction.TransportAction transportUpdateJobAction;

    @Inject
    public RestUpdateJobAction(Settings settings, RestController controller, UpdateJobAction.TransportAction transportUpdateJobAction) {
        super(settings);
        this.transportUpdateJobAction = transportUpdateJobAction;
        // NORELEASE Change method to POST and omit update from the endpoint
        controller.registerHandler(RestRequest.Method.PUT, "/engine/v2/jobs/{jobId}/update", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        UpdateJobAction.Request updateJobRequest = new UpdateJobAction.Request(
                restRequest.param(JOB_ID), RestActions.getRestContent(restRequest).utf8ToString());
        return channel -> transportUpdateJobAction.execute(updateJobRequest, new AcknowledgedRestListener<>(channel));
    }
}
