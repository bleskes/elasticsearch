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

import org.elasticsearch.action.admin.cluster.ClusterAction;
import org.elasticsearch.client.ClusterAdminClient;

public class DeleteLicenseAction extends ClusterAction<DeleteLicenseRequest, DeleteLicenseResponse, DeleteLicenseRequestBuilder> {

    public static final DeleteLicenseAction INSTANCE = new DeleteLicenseAction();
    public static final String NAME = "cluster:admin/license/delete";

    private DeleteLicenseAction() {
        super(NAME);
    }

    @Override
    public DeleteLicenseResponse newResponse() {
        return new DeleteLicenseResponse();
    }

    @Override
    public DeleteLicenseRequestBuilder newRequestBuilder(ClusterAdminClient client) {
        return new DeleteLicenseRequestBuilder(client);
    }
}