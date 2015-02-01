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

package org.elasticsearch.alerts.transport.actions.service;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.master.MasterNodeOperationRequestBuilder;
import org.elasticsearch.alerts.client.AlertsClient;
import org.elasticsearch.client.Client;

/**
 */
public class AlertsServiceRequestBuilder extends MasterNodeOperationRequestBuilder<AlertsServiceRequest, AlertsServiceResponse, AlertsServiceRequestBuilder, Client> {

    public AlertsServiceRequestBuilder(Client client) {
        super(client, new AlertsServiceRequest());
    }

    /**
     * Starts alerting if not already started.
     */
    public AlertsServiceRequestBuilder start() {
        request.start();
        return this;
    }

    /**
     * Stops alerting if not already stopped.
     */
    public AlertsServiceRequestBuilder stop() {
        request.stop();
        return this;
    }

    /**
     * Starts and stops alerting.
     */
    public AlertsServiceRequestBuilder restart() {
        request.restart();
        return this;
    }

    @Override
    protected void doExecute(ActionListener<AlertsServiceResponse> listener) {
        new AlertsClient(client).alertService(request, listener);
    }
}
