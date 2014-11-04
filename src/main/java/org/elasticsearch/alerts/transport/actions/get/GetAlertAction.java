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

import org.elasticsearch.action.ClientAction;
import org.elasticsearch.alerts.client.AlertsClient;
import org.elasticsearch.alerts.client.AlertsClientAction;
import org.elasticsearch.alerts.client.AlertsClientInterface;
import org.elasticsearch.client.Client;

/**
 */
public class GetAlertAction extends AlertsClientAction<GetAlertRequest, GetAlertResponse, GetAlertRequestBuilder> {

    public static final GetAlertAction INSTANCE = new GetAlertAction();
    public static final String NAME = "indices:data/read/alert/get";

    private GetAlertAction() {
        super(NAME);
    }

    @Override
    public GetAlertResponse newResponse() {
        return new GetAlertResponse();
    }

    @Override
    public GetAlertRequestBuilder newRequestBuilder(AlertsClientInterface client) {
        return new GetAlertRequestBuilder(client);
    }
}
