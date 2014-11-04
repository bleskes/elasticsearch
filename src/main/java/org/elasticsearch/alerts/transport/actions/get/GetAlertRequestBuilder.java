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

package org.elasticsearch.alerts.transport.actions.get;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.support.master.MasterNodeOperationRequestBuilder;
import org.elasticsearch.alerts.client.AlertsClient;
import org.elasticsearch.alerts.client.AlertsClientInterface;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.VersionType;

/**
 * A delete document action request builder.
 */
public class GetAlertRequestBuilder
        extends MasterNodeOperationRequestBuilder<GetAlertRequest, GetAlertResponse, GetAlertRequestBuilder, AlertsClientInterface> {


    public GetAlertRequestBuilder(AlertsClientInterface client, String alertName) {
        super(client, new GetAlertRequest(alertName));
    }


    public GetAlertRequestBuilder(AlertsClientInterface client) {
        super(client, new GetAlertRequest());
    }

    public GetAlertRequestBuilder setAlertName(String alertName) {
        request.alertName(alertName);
        return this;
    }

    /**
     * Sets the type of versioning to use. Defaults to {@link org.elasticsearch.index.VersionType#INTERNAL}.
     */
    public GetAlertRequestBuilder setVersionType(VersionType versionType) {
        request.versionType(versionType);
        return this;
    }

    @Override
    protected void doExecute(final ActionListener<GetAlertResponse> listener) {
        client.getAlert(request, listener);
    }
}
