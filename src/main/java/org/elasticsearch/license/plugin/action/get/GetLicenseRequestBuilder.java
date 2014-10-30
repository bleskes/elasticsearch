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

package org.elasticsearch.license.plugin.action.get;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.master.MasterNodeReadOperationRequestBuilder;
import org.elasticsearch.client.ClusterAdminClient;

public class GetLicenseRequestBuilder extends MasterNodeReadOperationRequestBuilder<GetLicenseRequest, GetLicenseResponse, GetLicenseRequestBuilder, ClusterAdminClient> {

    /**
     * Creates new get licenses request builder
     *
     * @param clusterAdminClient cluster admin client
     */
    public GetLicenseRequestBuilder(ClusterAdminClient clusterAdminClient) {
        super(clusterAdminClient, new GetLicenseRequest());
    }


    @Override
    protected void doExecute(ActionListener<GetLicenseResponse> listener) {
        client.execute(GetLicenseAction.INSTANCE, request, listener);
    }
}