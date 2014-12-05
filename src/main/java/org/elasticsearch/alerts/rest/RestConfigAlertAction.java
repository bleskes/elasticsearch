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

package org.elasticsearch.alerts.rest;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.alerts.AlertsStore;
import org.elasticsearch.alerts.ConfigurationManager;
import org.elasticsearch.alerts.client.AlertsClient;
import org.elasticsearch.alerts.transport.actions.config.ConfigAlertRequest;
import org.elasticsearch.alerts.transport.actions.config.ConfigAlertResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.*;
import org.elasticsearch.rest.action.support.RestBuilderListener;
import static org.elasticsearch.rest.RestStatus.CREATED;
import static org.elasticsearch.rest.RestStatus.OK;

/**
 */
public class RestConfigAlertAction extends BaseRestHandler {

    private final AlertsClient alertsClient;

    @Inject
    protected RestConfigAlertAction(Settings settings, RestController controller, Client client, AlertsClient alertsClient) {
        super(settings, controller, client);
        this.alertsClient = alertsClient;
        String path = AlertsStore.ALERT_INDEX + "/" + ConfigurationManager.CONFIG_TYPE + "/" + ConfigurationManager.GLOBAL_CONFIG_NAME;
        controller.registerHandler(RestRequest.Method.PUT, path, this);
        controller.registerHandler(RestRequest.Method.POST, path, this);
    }

    @Override
    protected void handleRequest(RestRequest request, RestChannel channel, Client client) throws Exception {
        ConfigAlertRequest configAlertRequest = new ConfigAlertRequest();
        configAlertRequest.setConfigSource(request.content());
        configAlertRequest.setConfigSourceUnsafe(request.contentUnsafe());
        alertsClient.alertConfig(configAlertRequest, new RestBuilderListener<ConfigAlertResponse>(channel) {
            @Override
            public RestResponse buildResponse(ConfigAlertResponse response, XContentBuilder builder) throws Exception {
                IndexResponse indexResponse = response.indexResponse();
                builder.startObject()
                        .field("_index", indexResponse.getIndex())
                        .field("_type", indexResponse.getType())
                        .field("_id", indexResponse.getId())
                        .field("_version", indexResponse.getVersion())
                        .field("created", indexResponse.isCreated());
                builder.endObject();
                RestStatus status = OK;
                if (indexResponse.isCreated()) {
                    status = CREATED;
                }
                return new BytesRestResponse(status, builder);
            }
        });
    }
}