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

package org.elasticsearch.alerts.transport.actions.stats;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.master.MasterNodeOperationRequestBuilder;
import org.elasticsearch.alerts.client.AlertsClientInterface;

/**
 * An alert stats document action request builder.
 */
public class AlertsStatsRequestBuilder
        extends MasterNodeOperationRequestBuilder<AlertsStatsRequest, AlertsStatsResponse, AlertsStatsRequestBuilder, AlertsClientInterface> {

    public AlertsStatsRequestBuilder(AlertsClientInterface client) {
        super(client, new AlertsStatsRequest());
    }


    @Override
    protected void doExecute(final ActionListener<AlertsStatsResponse> listener) {
        client.alertsStats(request, listener);
    }

}
