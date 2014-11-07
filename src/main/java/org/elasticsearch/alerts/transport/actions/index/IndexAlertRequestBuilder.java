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

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.master.MasterNodeOperationRequestBuilder;
import org.elasticsearch.alerts.client.AlertsClientInterface;
import org.elasticsearch.common.bytes.BytesReference;

/**
 */
public class IndexAlertRequestBuilder
        extends MasterNodeOperationRequestBuilder<IndexAlertRequest, IndexAlertResponse,
        IndexAlertRequestBuilder, AlertsClientInterface> {


    public IndexAlertRequestBuilder(AlertsClientInterface client) {
        super(client, new IndexAlertRequest());
    }


    public IndexAlertRequestBuilder(AlertsClientInterface client, String alertName) {
        super(client, new IndexAlertRequest());
        request.setAlertName(alertName);
    }

    public IndexAlertRequestBuilder setAlertName(String alertName){
        request.setAlertName(alertName);
        return this;
    }

    public IndexAlertRequestBuilder setAlertSource(BytesReference alertSource) {
        request.setAlertSource(alertSource);
        return this;
    }


    @Override
    protected void doExecute(ActionListener<IndexAlertResponse> listener) {
        client.indexAlert(request, listener);
    }
}
