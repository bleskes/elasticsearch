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

import org.elasticsearch.alerts.client.AlertsClientAction;
import org.elasticsearch.alerts.client.AlertsClient;

/**
 * This action gets an alert by name
 */
public class GetAlertAction extends AlertsClientAction<GetAlertRequest, GetAlertResponse, GetAlertRequestBuilder> {

    public static final GetAlertAction INSTANCE = new GetAlertAction();
    public static final String NAME = "indices:data/read/alert/get";

    private GetAlertAction() {
        super(NAME);
    }

    @Override
    public GetAlertResponse newResponse() {
        return new GetAlertResponse();
    }

    @Override
    public GetAlertRequestBuilder newRequestBuilder(AlertsClient client) {
        return new GetAlertRequestBuilder(client);
    }
}
