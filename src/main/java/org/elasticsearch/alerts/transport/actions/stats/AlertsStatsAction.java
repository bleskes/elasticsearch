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

import org.elasticsearch.alerts.client.AlertsClientAction;
import org.elasticsearch.alerts.client.AlertsClient;

/**
 */
public class AlertsStatsAction extends AlertsClientAction<AlertsStatsRequest, AlertsStatsResponse, AlertsStatsRequestBuilder> {

    public static final AlertsStatsAction INSTANCE = new AlertsStatsAction();
    public static final String NAME = "cluster/alerts/stats";

    private AlertsStatsAction() {
        super(NAME);
    }

    @Override
    public AlertsStatsResponse newResponse() {
        return new AlertsStatsResponse();
    }

    @Override
    public AlertsStatsRequestBuilder newRequestBuilder(AlertsClient client) {
        return new AlertsStatsRequestBuilder(client);
    }
}
