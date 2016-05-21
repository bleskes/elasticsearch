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

package org.elasticsearch.xpack.rest.action;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.action.support.RestBuilderListener;
import org.elasticsearch.xpack.XPackClient;
import org.elasticsearch.xpack.XPackFeatureSet;
import org.elasticsearch.xpack.action.XPackUsageRequestBuilder;
import org.elasticsearch.xpack.action.XPackUsageResponse;
import org.elasticsearch.xpack.rest.XPackRestHandler;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestStatus.OK;

public class RestXPackUsageAction extends XPackRestHandler {

    @Inject
    public RestXPackUsageAction(Settings settings, RestController controller, Client client) {
        super(settings, client);
        controller.registerHandler(GET, URI_BASE + "/usage", this);
    }

    @Override
    protected void handleRequest(RestRequest request, RestChannel restChannel, XPackClient client) throws Exception {
        new XPackUsageRequestBuilder(client.es()).execute(new RestBuilderListener<XPackUsageResponse>(restChannel) {
            @Override
            public RestResponse buildResponse(XPackUsageResponse response, XContentBuilder builder) throws Exception {
                builder.startObject();
                for (XPackFeatureSet.Usage usage : response.getUsages()) {
                    builder.field(usage.name(), usage);
                }
                builder.endObject();
                return new BytesRestResponse(OK, builder);
            }
        });
    }
}
