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

import org.elasticsearch.alerts.client.AlertsClient;
import org.elasticsearch.alerts.client.AlertsClientAction;

/**
 * This action acks an alert in memory, and the index
 */
public class AckAlertAction extends AlertsClientAction<AckAlertRequest, AckAlertResponse, AckAlertRequestBuilder> {

    public static final AckAlertAction INSTANCE = new AckAlertAction();
    public static final String NAME = "indices:data/write/alert/ack";

    private AckAlertAction() {
        super(NAME);
    }

    @Override
    public AckAlertResponse newResponse() {
        return new AckAlertResponse();
    }

    @Override
    public AckAlertRequestBuilder newRequestBuilder(AlertsClient client) {
        return new AckAlertRequestBuilder(client);
    }
}
