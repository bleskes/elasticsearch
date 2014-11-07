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

import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.alerts.client.AlertsClient;
import org.elasticsearch.alerts.transport.actions.delete.DeleteAlertRequest;
import org.elasticsearch.alerts.transport.actions.delete.DeleteAlertResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.*;
import org.elasticsearch.rest.action.support.RestBuilderListener;

import static org.elasticsearch.rest.RestRequest.Method.DELETE;
import static org.elasticsearch.rest.RestStatus.NOT_FOUND;
import static org.elasticsearch.rest.RestStatus.OK;

/**
 */
public class RestDeleteAlertAction extends BaseRestHandler {

    private final AlertsClient alertsClient;

    @Inject
    public RestDeleteAlertAction(Settings settings, RestController controller, Client client, AlertsClient alertsClient) {
        super(settings, controller, client);
        this.alertsClient = alertsClient;
        controller.registerHandler(DELETE, "/_alert/{name}", this);
    }

    @Override
    protected void handleRequest(RestRequest request, RestChannel channel, Client client) throws Exception {
        DeleteAlertRequest indexAlertRequest = new DeleteAlertRequest();
        indexAlertRequest.setAlertName(request.param("name"));
        alertsClient.deleteAlert(indexAlertRequest, new RestBuilderListener<DeleteAlertResponse>(channel) {
            @Override
            public RestResponse buildResponse(DeleteAlertResponse result, XContentBuilder builder) throws Exception {
                DeleteResponse deleteResponse = result.deleteResponse();
                builder.startObject()
                        .field("found", deleteResponse.isFound())
                        .field("_index", deleteResponse.getIndex())
                        .field("_type", deleteResponse.getType())
                        .field("_id", deleteResponse.getId())
                        .field("_version", deleteResponse.getVersion())
                        .endObject();
                RestStatus status = OK;
                if (!deleteResponse.isFound()) {
                    status = NOT_FOUND;
                }
                return new BytesRestResponse(status, builder);
            }
        });
    }
}
