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

import org.elasticsearch.alerts.client.AlertsAction;
import org.elasticsearch.client.Client;

/**
 */
public class AlertsServiceAction extends AlertsAction<AlertsServiceRequest, AlertsServiceResponse, AlertsServiceRequestBuilder> {

    public static final AlertsServiceAction INSTANCE = new AlertsServiceAction();
    public static final String NAME = "cluster:admin/alerts/service";

    private AlertsServiceAction() {
        super(NAME);
    }

    @Override
    public AlertsServiceResponse newResponse() {
        return new AlertsServiceResponse();
    }

    @Override
    public AlertsServiceRequestBuilder newRequestBuilder(Client client) {
        return new AlertsServiceRequestBuilder(client);
    }

}
