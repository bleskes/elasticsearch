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
package org.elasticsearch.xpack.prelert.rest.schedulers;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.AcknowledgedRestListener;
import org.elasticsearch.xpack.prelert.action.StopJobSchedulerAction;

import java.io.IOException;

public class RestStopJobSchedulerAction extends BaseRestHandler {

    private static final ParseField JOB_ID = new ParseField("jobId");

    private final StopJobSchedulerAction.TransportAction transportJobSchedulerAction;

    @Inject
    public RestStopJobSchedulerAction(Settings settings, RestController controller,
                                      StopJobSchedulerAction.TransportAction transportJobSchedulerAction) {
        super(settings);
        this.transportJobSchedulerAction = transportJobSchedulerAction;
        controller.registerHandler(RestRequest.Method.POST, "/engine/v2/schedulers/{jobId}/stop", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        StopJobSchedulerAction.Request jobSchedulerRequest = new StopJobSchedulerAction.Request(
                restRequest.param(JOB_ID.getPreferredName()));
        return channel -> transportJobSchedulerAction.execute(jobSchedulerRequest, new AcknowledgedRestListener<>(channel));
    }
}
