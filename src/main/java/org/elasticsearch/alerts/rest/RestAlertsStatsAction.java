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

package org.elasticsearch.alerts.rest;

import org.elasticsearch.alerts.client.AlertsClient;
import org.elasticsearch.alerts.transport.actions.stats.AlertsStatsRequest;
import org.elasticsearch.alerts.transport.actions.stats.AlertsStatsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.*;
import org.elasticsearch.rest.action.support.RestBuilderListener;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestStatus.OK;

/**
 * The RestAction for alerts stats
 */
public class RestAlertsStatsAction extends BaseRestHandler {

    private final AlertsClient alertsClient;

    @Inject
    protected RestAlertsStatsAction(Settings settings, RestController controller, Client client, AlertsClient alertsClient) {
        super(settings, controller, client);
        this.alertsClient = alertsClient;
        controller.registerHandler(GET, "/_alert/stats", this);
    }

    @Override
    protected void handleRequest(RestRequest request, RestChannel restChannel, Client client) throws Exception {
        AlertsStatsRequest statsRequest = new AlertsStatsRequest();
        alertsClient.alertsStats(statsRequest, new RestBuilderListener<AlertsStatsResponse>(restChannel) {
            @Override
            public RestResponse buildResponse(AlertsStatsResponse alertsStatsResponse, XContentBuilder builder) throws Exception {
                builder.startObject()
                        .field("alert_manager_started", alertsStatsResponse.isAlertManagerStarted())
                        .field("alert_action_manager_started", alertsStatsResponse.isAlertActionManagerStarted())
                        .field("alert_action_queue_size", alertsStatsResponse.getAlertActionManagerQueueSize())
                        .field("number_of_alerts", alertsStatsResponse.getNumberOfRegisteredAlerts());
                return new BytesRestResponse(OK, builder);

            }
        });
    }
}
