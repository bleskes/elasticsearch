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

package org.elasticsearch.xpack.monitoring.rest.action;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.action.support.RestActions;
import org.elasticsearch.rest.action.support.RestBuilderListener;
import org.elasticsearch.xpack.XPackClient;
import org.elasticsearch.xpack.monitoring.action.MonitoringBulkRequestBuilder;
import org.elasticsearch.xpack.monitoring.action.MonitoringBulkResponse;
import org.elasticsearch.xpack.monitoring.rest.MonitoringRestHandler;

import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestRequest.Method.PUT;

public class RestMonitoringBulkAction extends MonitoringRestHandler {

    public static final String MONITORING_ID = "system_id";
    public static final String MONITORING_VERSION = "system_version";

    @Inject
    public RestMonitoringBulkAction(Settings settings, RestController controller, Client client) {
        super(settings, client);
        controller.registerHandler(POST, URI_BASE + "/_bulk", this);
        controller.registerHandler(PUT, URI_BASE + "/_bulk", this);
        controller.registerHandler(POST, URI_BASE + "/{type}/_bulk", this);
        controller.registerHandler(PUT, URI_BASE + "/{type}/_bulk", this);
    }

    @Override
    protected void handleRequest(RestRequest request, RestChannel channel, XPackClient client) throws Exception {
        String defaultType = request.param("type");

        String id = request.param(MONITORING_ID);
        if (Strings.hasLength(id) == false) {
            throw new IllegalArgumentException("no monitoring id for monitoring bulk request");
        }
        String version = request.param(MONITORING_VERSION);
        if (Strings.hasLength(version) == false) {
            throw new IllegalArgumentException("no monitoring version for monitoring bulk request");
        }

        if (!RestActions.hasBodyContent(request)) {
            throw new ElasticsearchParseException("no body content for monitoring bulk request");
        }

        MonitoringBulkRequestBuilder requestBuilder = client.monitoring().prepareMonitoringBulk();
        requestBuilder.add(request.content(), id, version, defaultType);
        requestBuilder.execute(new RestBuilderListener<MonitoringBulkResponse>(channel) {
            @Override
            public RestResponse buildResponse(MonitoringBulkResponse response, XContentBuilder builder) throws Exception {
                builder.startObject();
                builder.field(Fields.TOOK, response.getTookInMillis());

                MonitoringBulkResponse.Error error = response.getError();
                builder.field(Fields.ERRORS, error != null);

                if (error != null) {
                    builder.field(Fields.ERROR, response.getError());
                }
                builder.endObject();
                return new BytesRestResponse(response.status(), builder);
            }
        });
    }

    static final class Fields {
        static final String TOOK = "took";
        static final String ERRORS = "errors";
        static final String ERROR = "error";
    }
}
