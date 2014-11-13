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
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.TransportAction;
import org.elasticsearch.alerts.AlertsStore;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;

/**
 * Performs the get operation.
 */
public class TransportGetAlertAction extends TransportAction<GetAlertRequest,  GetAlertResponse> {

    private final Client client;

    @Inject
    public TransportGetAlertAction(Settings settings, ThreadPool threadPool, ActionFilters actionFilters, Client client) {
        super(settings, GetAlertAction.NAME, threadPool, actionFilters);
        this.client = client;
    }

    @Override
    protected void doExecute(GetAlertRequest request, ActionListener<GetAlertResponse> listener) {
        try {
            GetResponse getResponse = client.prepareGet(AlertsStore.ALERT_INDEX, AlertsStore.ALERT_TYPE, request.alertName())
                    .setVersion(request.version())
                    .setVersionType(request.versionType()).execute().actionGet();
            GetAlertResponse response = new GetAlertResponse(getResponse);
            listener.onResponse(response);
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
}
