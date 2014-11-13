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
import org.elasticsearch.alerts.client.AlertsClient;
import org.elasticsearch.alerts.transport.actions.index.IndexAlertRequest;
import org.elasticsearch.alerts.transport.actions.index.IndexAlertResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.*;
import org.elasticsearch.rest.action.support.RestBuilderListener;

import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestRequest.Method.PUT;
import static org.elasticsearch.rest.RestStatus.CREATED;
import static org.elasticsearch.rest.RestStatus.OK;

/**
 */
public class RestIndexAlertAction extends BaseRestHandler {

    private final AlertsClient alertsClient;

    @Inject
    public RestIndexAlertAction(Settings settings, RestController controller, Client client, AlertsClient alertsClient) {
        super(settings, controller, client);
        this.alertsClient = alertsClient;
        controller.registerHandler(POST, "/_alert/{name}", this);
        controller.registerHandler(PUT, "/_alert/{name}", this);
    }

    @Override
    protected void handleRequest(RestRequest request, RestChannel channel, Client client) throws Exception {
        IndexAlertRequest indexAlertRequest = new IndexAlertRequest();
        indexAlertRequest.setAlertName(request.param("name"));
        indexAlertRequest.setAlertSource(request.content(), request.contentUnsafe());
        alertsClient.indexAlert(indexAlertRequest, new RestBuilderListener<IndexAlertResponse>(channel) {
            @Override
            public RestResponse buildResponse(IndexAlertResponse response, XContentBuilder builder) throws Exception {
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
