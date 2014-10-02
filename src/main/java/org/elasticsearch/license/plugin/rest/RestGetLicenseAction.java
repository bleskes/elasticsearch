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
import org.elasticsearch.cluster.metadata.RepositoriesMetaData;
import org.elasticsearch.cluster.metadata.RepositoryMetaData;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.*;

import static org.elasticsearch.client.Requests.getRepositoryRequest;
import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestStatus.OK;

public class RestGetLicenseAction extends BaseRestHandler {

    @Inject
    public RestGetLicenseAction(Settings settings, RestController controller, Client client) {
        super(settings, controller, client);
        controller.registerHandler(GET, "/_cluster/license", this);
    }

    @Override
    public void handleRequest(final RestRequest request, final RestChannel channel, final Client client) {
        final String[] repositories = request.paramAsStringArray("repository", Strings.EMPTY_ARRAY);
        //TODO: implement after custom metadata impl
        /*
        GetRepositoriesRequest getRepositoriesRequest = getRepositoryRequest(repositories);
        getRepositoriesRequest.masterNodeTimeout(request.paramAsTime("master_timeout", getRepositoriesRequest.masterNodeTimeout()));
        getRepositoriesRequest.local(request.paramAsBoolean("local", getRepositoriesRequest.local()));
        client.admin().cluster().getRepositories(getRepositoriesRequest, new RestBuilderListener<GetRepositoriesResponse>(channel) {
            @Override
            public RestResponse buildResponse(GetRepositoriesResponse response, XContentBuilder builder) throws Exception {
                builder.startObject();
                for (RepositoryMetaData repositoryMetaData : response.repositories()) {
                    RepositoriesMetaData.FACTORY.toXContent(repositoryMetaData, builder, request);
                }
                builder.endObject();

                return new BytesRestResponse(OK, builder);
            }
        });*/
    }
}
