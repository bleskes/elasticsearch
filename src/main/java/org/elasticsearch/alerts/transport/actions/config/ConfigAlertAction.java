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

import org.elasticsearch.alerts.client.AlertsAction;
import org.elasticsearch.client.Client;

/**
 * This action deletes an alert from in memory, the scheduler and the index
 */
public class ConfigAlertAction extends AlertsAction<ConfigAlertRequest, ConfigAlertResponse, ConfigAlertRequestBuilder> {

    public static final ConfigAlertAction INSTANCE = new ConfigAlertAction();
    public static final String NAME = "indices:data/write/alert/config";

    private ConfigAlertAction() {
        super(NAME);
    }

    @Override
    public ConfigAlertResponse newResponse() {
        return new ConfigAlertResponse();
    }

    @Override
    public ConfigAlertRequestBuilder newRequestBuilder(Client client) {
        return new ConfigAlertRequestBuilder(client);
    }

}
