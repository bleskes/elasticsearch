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
import org.elasticsearch.alerts.client.AlertsClient;
import org.elasticsearch.common.bytes.BytesReference;

/**
 * A Builder to build a PutAlertRequest
 */
public class PutAlertRequestBuilder
        extends MasterNodeOperationRequestBuilder<PutAlertRequest, PutAlertResponse,
        PutAlertRequestBuilder, AlertsClient> {

    /**
     * The Constructor for the PutAlertRequestBuilder
     * @param client The client that will execute the action
     */
    public PutAlertRequestBuilder(AlertsClient client) {
        super(client, new PutAlertRequest());
    }

    /**
     * The Constructor for the PutAlertRequestBuilder
     * @param client The client that will execute the action
     * @param alertName The name of the alert to be put
     */
    public PutAlertRequestBuilder(AlertsClient client, String alertName) {
        super(client, new PutAlertRequest());
        request.setAlertName(alertName);
    }

    /**
     * Sets the alert name to be created
     * @param alertName
     * @return
     */
    public PutAlertRequestBuilder setAlertName(String alertName){
        request.setAlertName(alertName);
        return this;
    }

    /**
     * Sets the source of the alert to be created
     * @param alertSource
     * @return
     */
    public PutAlertRequestBuilder setAlertSource(BytesReference alertSource) {
        request.setAlertSource(alertSource);
        return this;
    }


    @Override
    protected void doExecute(ActionListener<PutAlertResponse> listener) {
        client.putAlert(request, listener);
    }
}
