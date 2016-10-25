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
import org.elasticsearch.xpack.prelert.action.DeleteJobAction;
import org.elasticsearch.xpack.prelert.job.JobDetails;

import java.io.IOException;

public class RestDeleteJobAction extends BaseRestHandler {

    private final DeleteJobAction.TransportAction transportDeleteJobAction;

    @Inject
    public RestDeleteJobAction(Settings settings, RestController controller, DeleteJobAction.TransportAction transportDeleteJobAction) {
        super(settings);
        this.transportDeleteJobAction = transportDeleteJobAction;
        controller.registerHandler(RestRequest.Method.DELETE, "/engine/v2/jobs/{" + JobDetails.ID.getPreferredName() + "}", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        DeleteJobAction.Request deleteJobRequest = new DeleteJobAction.Request(restRequest.param(JobDetails.ID.getPreferredName()));
        return channel -> transportDeleteJobAction.execute(deleteJobRequest, new AcknowledgedRestListener<>(channel));
    }
}
