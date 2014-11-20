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

package org.elasticsearch.alerts.transport.actions.ack;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.master.MasterNodeOperationRequestBuilder;
import org.elasticsearch.alerts.client.AlertsClient;

/**
 * A ack alert action request builder.
 */
public class AckAlertRequestBuilder
        extends MasterNodeOperationRequestBuilder<AckAlertRequest, AckAlertResponse, AckAlertRequestBuilder, AlertsClient> {

    public AckAlertRequestBuilder(AlertsClient client) {
        super(client, new AckAlertRequest());
    }

    public AckAlertRequestBuilder(AlertsClient client, String alertName) {
        super(client, new AckAlertRequest(alertName));
    }

    /**
     * Sets the name of the alert to be ack
     * @param alertName
     * @return
     */
    public AckAlertRequestBuilder setAlertName(String alertName) {
        this.request().setAlertName(alertName);
        return this;
    }

    @Override
    protected void doExecute(final ActionListener<AckAlertResponse> listener) {
        client.ackAlert(request, listener);
    }

}
