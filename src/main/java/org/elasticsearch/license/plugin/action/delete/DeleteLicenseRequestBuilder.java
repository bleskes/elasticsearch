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

package org.elasticsearch.license.plugin.action.delete;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.master.AcknowledgedRequestBuilder;
import org.elasticsearch.client.ClusterAdminClient;

import java.util.Set;

public class DeleteLicenseRequestBuilder extends AcknowledgedRequestBuilder<DeleteLicenseRequest, DeleteLicenseResponse, DeleteLicenseRequestBuilder, ClusterAdminClient> {

    /**
     * Creates new get licenses request builder
     *
     * @param clusterAdminClient cluster admin client
     */
    public DeleteLicenseRequestBuilder(ClusterAdminClient clusterAdminClient) {
        super(clusterAdminClient, new DeleteLicenseRequest());
    }

    public DeleteLicenseRequestBuilder setFeatures(Set<String> features) {
        request.features(features);
        return this;
    }


    @Override
    protected void doExecute(ActionListener<DeleteLicenseResponse> listener) {
        client.execute(DeleteLicenseAction.INSTANCE, request, listener);
    }
}