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

package org.elasticsearch.alerts.transport.actions.index;

import org.elasticsearch.alerts.client.AlertsClientAction;
import org.elasticsearch.alerts.client.AlertsClientInterface;

/**
 */
public class IndexAlertAction extends AlertsClientAction<IndexAlertRequest, IndexAlertResponse, IndexAlertRequestBuilder> {

    public static final IndexAlertAction INSTANCE = new IndexAlertAction();
    public static final String NAME = "indices:data/write/alert/index";

    private IndexAlertAction() {
        super(NAME);
    }


    @Override
    public IndexAlertRequestBuilder newRequestBuilder(AlertsClientInterface client) {
        return new IndexAlertRequestBuilder(client);
    }

    @Override
    public IndexAlertResponse newResponse() {
        return new IndexAlertResponse();
    }
}
