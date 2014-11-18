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

import org.elasticsearch.alerts.client.AlertsClientAction;
import org.elasticsearch.alerts.client.AlertsClient;

/**
 * This action puts an alert into the alert index and adds it to the scheduler
 */
public class PutAlertAction extends AlertsClientAction<PutAlertRequest, PutAlertResponse, PutAlertRequestBuilder> {

    public static final PutAlertAction INSTANCE = new PutAlertAction();
    public static final String NAME = "indices:data/write/alert/put";

    private PutAlertAction() {
        super(NAME);
    }

    /**
     * The Alerts Client
     * @param client
     * @return A PutAlertRequestBuilder
     */
    @Override
    public PutAlertRequestBuilder newRequestBuilder(AlertsClient client) {
        return new PutAlertRequestBuilder(client);
    }

    @Override
    public PutAlertResponse newResponse() {
        return new PutAlertResponse();
    }
}
