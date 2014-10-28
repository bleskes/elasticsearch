/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.license.plugin.rest;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.license.core.ESLicenses;
import org.elasticsearch.license.plugin.action.get.GetLicenseAction;
import org.elasticsearch.license.plugin.action.get.GetLicenseRequest;
import org.elasticsearch.license.plugin.action.get.GetLicenseResponse;
import org.elasticsearch.rest.*;
import org.elasticsearch.rest.action.support.RestBuilderListener;

import java.util.Map;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestStatus.OK;

public class RestGetLicenseAction extends BaseRestHandler {

    @Inject
    public RestGetLicenseAction(Settings settings, RestController controller, Client client) {
        super(settings, controller, client);
        controller.registerHandler(GET, "/_licenses", this);
    }

    /**
     * Output Format:
     *   {
     *     "licenses" : [
     *        {
     *         "uid" : ...,
     *          "type" : ...,
     *          "subscription_type" :...,
     *          "issued_to" : ... (cluster name if one-time trial license, else value from signed license),
     *          "issue_date" : YY-MM-DD (date string in UTC),
     *          "expiry_date" : YY-MM-DD (date string in UTC),
     *          "feature" : ...,
     *          "max_nodes" : ...
     *        },
     *        {...}
     *      ]
     *   }p
     *
     * There will be only one license displayed per feature, the selected license will have the latest expiry_date
     * out of all other licenses for the feature.
     *
     * The licenses are sorted by latest issue_date
     */

    @Override
    public void handleRequest(final RestRequest request, final RestChannel channel, final Client client) {
        final Map<String, String> overrideParams = ImmutableMap.of(ESLicenses.OMIT_SIGNATURE, "true");
        final ToXContent.Params params = new ToXContent.DelegatingMapParams(overrideParams, request);
        client.admin().cluster().execute(GetLicenseAction.INSTANCE, new GetLicenseRequest(), new RestBuilderListener<GetLicenseResponse>(channel) {
            @Override
            public RestResponse buildResponse(GetLicenseResponse response, XContentBuilder builder) throws Exception {
                ESLicenses.toXContent(response.licenses(), builder, params);
                return new BytesRestResponse(OK, builder);
            }
        });
    }

}
