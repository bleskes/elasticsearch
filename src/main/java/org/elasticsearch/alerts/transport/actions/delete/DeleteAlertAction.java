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

import org.elasticsearch.alerts.client.AlertsClientAction;
import org.elasticsearch.alerts.client.AlertsClient;

/**
 * This action deletes an alert from in memory, the scheduler and the index
 */
public class DeleteAlertAction extends AlertsClientAction<DeleteAlertRequest, DeleteAlertResponse, DeleteAlertRequestBuilder> {

    public static final DeleteAlertAction INSTANCE = new DeleteAlertAction();
    public static final String NAME = "indices:data/write/alert/delete";

    private DeleteAlertAction() {
        super(NAME);
    }

    @Override
    public DeleteAlertResponse newResponse() {
        return new DeleteAlertResponse();
    }

    @Override
    public DeleteAlertRequestBuilder newRequestBuilder(AlertsClient client) {
        return new DeleteAlertRequestBuilder(client);
    }
}
