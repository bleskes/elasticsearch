/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated
 *  All Rights Reserved.
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
package org.elasticsearch.license.plugin.action.put;

import org.elasticsearch.action.admin.cluster.ClusterAction;
import org.elasticsearch.client.ClusterAdminClient;

public class PutLicenseAction extends ClusterAction<PutLicenseRequest, PutLicenseResponse, PutLicenseRequestBuilder> {

    public static final PutLicenseAction INSTANCE = new PutLicenseAction();
    public static final String NAME = "cluster:admin/license/put";

    private PutLicenseAction() {
        super(NAME);
    }

    @Override
    public PutLicenseResponse newResponse() {
        return new PutLicenseResponse();
    }

    @Override
    public PutLicenseRequestBuilder newRequestBuilder(ClusterAdminClient client) {
        return new PutLicenseRequestBuilder(client);
    }
}
