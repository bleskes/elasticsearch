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
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.plugin.action.put.PutLicenseAction;
import org.elasticsearch.license.plugin.action.put.PutLicenseRequest;
import org.elasticsearch.license.plugin.action.put.PutLicenseResponse;
import org.elasticsearch.license.plugin.action.put.TransportPutLicenseAction;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.support.AcknowledgedRestListener;

import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestRequest.Method.PUT;

public class RestPutLicenseAction extends BaseRestHandler {

    private final TransportPutLicenseAction transportPutLicensesAction;

    @Inject
    public RestPutLicenseAction(Settings settings, RestController controller, Client client, TransportPutLicenseAction transportPutLicenseAction) {
        super(settings, controller, client);
        controller.registerHandler(PUT, "/_cluster/license", this);
        controller.registerHandler(POST, "/_cluster/license", this);
        this.transportPutLicensesAction = transportPutLicenseAction;
    }


    @Override
    public void handleRequest(final RestRequest request, final RestChannel channel, final Client client) {
        PutLicenseRequest putLicenseRequest = new PutLicenseRequest();
        putLicenseRequest.listenerThreaded(false);
        putLicenseRequest.license(request.content().toUtf8());
        transportPutLicensesAction.execute(putLicenseRequest, new AcknowledgedRestListener<PutLicenseResponse>(channel));
     //   client.admin().cluster().execute(PutLicenseAction.INSTANCE, putLicenseRequest, );
    }
}
