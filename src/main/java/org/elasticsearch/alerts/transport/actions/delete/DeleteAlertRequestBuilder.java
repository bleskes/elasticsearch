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

package org.elasticsearch.alerts.transport.actions.delete;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.master.MasterNodeOperationRequestBuilder;
import org.elasticsearch.alerts.client.AlertsClientInterface;

/**
 * A delete document action request builder.
 */
public class DeleteAlertRequestBuilder
        extends MasterNodeOperationRequestBuilder<DeleteAlertRequest, DeleteAlertResponse, DeleteAlertRequestBuilder, AlertsClientInterface> {

    public DeleteAlertRequestBuilder(AlertsClientInterface client) {
        super(client, new DeleteAlertRequest());
    }

    public DeleteAlertRequestBuilder(AlertsClientInterface client, String alertName) {
        super(client, new DeleteAlertRequest(alertName));
    }

    public DeleteAlertRequestBuilder setAlertName(String alertName) {
        this.request().setAlertName(alertName);
        return this;
    }

    @Override
    protected void doExecute(final ActionListener<DeleteAlertResponse> listener) {
        client.deleteAlert(request, listener);
    }

}
