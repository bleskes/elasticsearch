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

package org.elasticsearch.alerts.transport.actions.config;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.master.MasterNodeOperationRequestBuilder;
import org.elasticsearch.alerts.client.AlertsClient;

/**
 * A alert config action request builder.
 */
public class ConfigAlertRequestBuilder
        extends MasterNodeOperationRequestBuilder<ConfigAlertRequest, ConfigAlertResponse, ConfigAlertRequestBuilder, AlertsClient> {

    public ConfigAlertRequestBuilder(AlertsClient client) {
        super(client, new ConfigAlertRequest());
    }

    public ConfigAlertRequestBuilder(AlertsClient client, String alertName) {
        super(client, new ConfigAlertRequest(alertName));
    }

    /**
     * Sets the name of the config to be modified
     * @param configName
     * @return
     */
    public ConfigAlertRequestBuilder setAlertName(String configName) {
        this.request().setConfigName(configName);
        return this;
    }

    @Override
    protected void doExecute(final ActionListener<ConfigAlertResponse> listener) {
        client.alertConfig(request, listener);
    }

}
