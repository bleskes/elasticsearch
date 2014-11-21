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
import org.elasticsearch.alerts.transport.actions.service.AlertsServiceRequest;
import org.elasticsearch.alerts.transport.actions.service.AlertsServiceResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.*;
import org.elasticsearch.rest.action.support.AcknowledgedRestListener;

/**
 */
public class RestAlertServiceAction extends BaseRestHandler {

    private final AlertsClient alertsClient;

    @Inject
    protected RestAlertServiceAction(Settings settings, RestController controller, Client client, AlertsClient alertsClient) {
        super(settings, controller, client);
        this.alertsClient = alertsClient;
        controller.registerHandler(RestRequest.Method.PUT, AlertsStore.ALERT_INDEX + "/_restart", this);
        controller.registerHandler(RestRequest.Method.PUT, AlertsStore.ALERT_INDEX + "/_start", new StartRestHandler(settings, controller, client));
        controller.registerHandler(RestRequest.Method.PUT, AlertsStore.ALERT_INDEX + "/_stop", new StopRestHandler(settings, controller, client));
    }

    @Override
    protected void handleRequest(RestRequest request, RestChannel channel, Client client) throws Exception {
        AlertsServiceRequest serviceRequest = new AlertsServiceRequest();
        serviceRequest.restart();
        alertsClient.alertService(serviceRequest, new AcknowledgedRestListener<AlertsServiceResponse>(channel));
    }

    final class StartRestHandler extends BaseRestHandler {

        public StartRestHandler(Settings settings, RestController controller, Client client) {
            super(settings, controller, client);
        }

        @Override
        public void handleRequest(RestRequest request, RestChannel channel, Client client) throws Exception {
            AlertsServiceRequest serviceRequest = new AlertsServiceRequest();
            serviceRequest.start();
            alertsClient.alertService(serviceRequest, new AcknowledgedRestListener<AlertsServiceResponse>(channel));
        }
    }

    final class StopRestHandler extends BaseRestHandler {

        public StopRestHandler(Settings settings, RestController controller, Client client) {
            super(settings, controller, client);
        }

        @Override
        public void handleRequest(RestRequest request, RestChannel channel, Client client) throws Exception {
            AlertsServiceRequest serviceRequest = new AlertsServiceRequest();
            serviceRequest.stop();
            alertsClient.alertService(serviceRequest, new AcknowledgedRestListener<AlertsServiceResponse>(channel));
        }
    }
}
