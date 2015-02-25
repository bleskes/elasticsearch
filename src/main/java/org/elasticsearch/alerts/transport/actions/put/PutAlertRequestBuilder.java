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

package org.elasticsearch.alerts.transport.actions.put;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.master.MasterNodeOperationRequestBuilder;
import org.elasticsearch.alerts.client.AlertSourceBuilder;
import org.elasticsearch.alerts.client.AlertsClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.bytes.BytesReference;

/**
 * A Builder to build a PutAlertRequest
 */
public class PutAlertRequestBuilder extends MasterNodeOperationRequestBuilder<PutAlertRequest, PutAlertResponse, PutAlertRequestBuilder, Client> {

    public PutAlertRequestBuilder(Client client) {
        super(client, new PutAlertRequest());
    }

    public PutAlertRequestBuilder(Client client, String alertName) {
        super(client, new PutAlertRequest());
        request.setAlertName(alertName);
    }

    /**
     * @param alertName The alert name to be created
     */
    public PutAlertRequestBuilder alertName(String alertName){
        request.setAlertName(alertName);
        return this;
    }

    /**
     * @param source the source of the alert to be created
     */
    public PutAlertRequestBuilder source(BytesReference source) {
        request.source(source);
        return this;
    }

    /**
     * @param source the source of the alert to be created
     */
    public PutAlertRequestBuilder source(AlertSourceBuilder source) {
        request.source(source);
        return this;
    }

    @Override
    protected void doExecute(ActionListener<PutAlertResponse> listener) {
        new AlertsClient(client).putAlert(request, listener);
    }
}
