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

import org.elasticsearch.alerts.AlertsStore;
import org.elasticsearch.alerts.client.AlertsClient;
import org.elasticsearch.alerts.transport.actions.ack.AckAlertRequest;
import org.elasticsearch.alerts.transport.actions.ack.AckAlertResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.*;
import org.elasticsearch.rest.action.support.RestBuilderListener;

/**
 * The rest action to ack an alert
 */
public class RestAckAlertAction extends BaseRestHandler {

    private final AlertsClient alertsClient;

    @Inject
    protected RestAckAlertAction(Settings settings, RestController controller, Client client, AlertsClient alertsClient) {
        super(settings, controller, client);
        this.alertsClient = alertsClient;
        controller.registerHandler(RestRequest.Method.PUT, AlertsStore.ALERT_INDEX + "/{name}/_ack", this);
    }

    @Override
    protected void handleRequest(RestRequest request, RestChannel restChannel, Client client) throws Exception {
        final AckAlertRequest ackAlertRequest = new AckAlertRequest();
        ackAlertRequest.setAlertName(request.param("name"));
        alertsClient.ackAlert(ackAlertRequest, new RestBuilderListener<AckAlertResponse>(restChannel) {

            @Override
            public RestResponse buildResponse(AckAlertResponse ackAlertResponse, XContentBuilder builder) throws Exception {
                builder.startObject();
                builder.field(AlertsStore.ACK_STATE_FIELD.getPreferredName(), ackAlertResponse.getAlertAckState().toString());
                builder.endObject();
                return new BytesRestResponse(RestStatus.OK, builder);
            }
        });
    }
    
}
