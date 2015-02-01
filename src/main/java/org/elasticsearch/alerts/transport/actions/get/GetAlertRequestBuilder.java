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

package org.elasticsearch.alerts.transport.actions.get;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.alerts.client.AlertsClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.VersionType;

/**
 * A delete document action request builder.
 */
public class GetAlertRequestBuilder extends ActionRequestBuilder<GetAlertRequest, GetAlertResponse, GetAlertRequestBuilder, Client> {

    public GetAlertRequestBuilder(Client client, String alertName) {
        super(client, new GetAlertRequest(alertName));
    }


    public GetAlertRequestBuilder(Client client) {
        super(client, new GetAlertRequest());
    }

    public GetAlertRequestBuilder setAlertName(String alertName) {
        request.alertName(alertName);
        return this;
    }

    /**
     * Sets the type of versioning to use. Defaults to {@link org.elasticsearch.index.VersionType#INTERNAL}.
     */
    public GetAlertRequestBuilder setVersionType(VersionType versionType) {
        request.versionType(versionType);
        return this;
    }

    @Override
    protected void doExecute(final ActionListener<GetAlertResponse> listener) {
        new AlertsClient(client).getAlert(request, listener);
    }
}
