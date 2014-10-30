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