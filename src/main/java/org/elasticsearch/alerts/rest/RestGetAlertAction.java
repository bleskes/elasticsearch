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

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.alerts.AlertsStore;
import org.elasticsearch.alerts.client.AlertsClient;
import org.elasticsearch.alerts.transport.actions.get.GetAlertRequest;
import org.elasticsearch.alerts.transport.actions.get.GetAlertResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.*;
import org.elasticsearch.rest.action.support.RestBuilderListener;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestStatus.NOT_FOUND;
import static org.elasticsearch.rest.RestStatus.OK;

/**
 * The rest action to get an alert
 */
public class RestGetAlertAction extends BaseRestHandler {

    private final AlertsClient alertsClient;

    @Inject
    public RestGetAlertAction(Settings settings, RestController controller, Client client, AlertsClient alertsClient) {
        super(settings, controller, client);
        this.alertsClient = alertsClient;
        controller.registerHandler(GET, AlertsStore.ALERT_INDEX + "/alert/{name}", this);
    }

    @Override
    protected void handleRequest(RestRequest request, RestChannel channel, Client client) throws Exception {
        final GetAlertRequest getAlertRequest = new GetAlertRequest();
        getAlertRequest.alertName(request.param("name"));
        alertsClient.getAlert(getAlertRequest, new RestBuilderListener<GetAlertResponse>(channel) {
            @Override
            public RestResponse buildResponse(GetAlertResponse result, XContentBuilder builder) throws Exception {
                GetResponse getResponse = result.getResponse();
                builder.startObject()
                        .field("found", getResponse.isExists())
                        .field("_index", getResponse.getIndex())
                        .field("_type", getResponse.getType())
                        .field("_id", getResponse.getId())
                        .field("_version", getResponse.getVersion())
                        .field("alert", getResponse.getSource())
                        .endObject();

                RestStatus status = OK;
                if (!getResponse.isExists()) {
                    status = NOT_FOUND;
                }
                return new BytesRestResponse(status, builder);
            }
        });
    }
}