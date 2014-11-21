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

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.master.AcknowledgedRequestBuilder;
import org.elasticsearch.client.ClusterAdminClient;
import org.elasticsearch.license.core.License;

import java.util.List;

/**
 * Register license request builder
 */
public class PutLicenseRequestBuilder extends AcknowledgedRequestBuilder<PutLicenseRequest, PutLicenseResponse, PutLicenseRequestBuilder, ClusterAdminClient> {

    /**
     * Constructs register license request
     *
     * @param clusterAdminClient cluster admin client
     */
    public PutLicenseRequestBuilder(ClusterAdminClient clusterAdminClient) {
        super(clusterAdminClient, new PutLicenseRequest());
    }

    /**
     * Sets the license
     *
     * @param licenses license
     * @return this builder
     */
    public PutLicenseRequestBuilder setLicense(List<License> licenses) {
        request.licenses(licenses);
        return this;
    }

    public PutLicenseRequestBuilder setLicense(String licenseSource) {
        request.licenses(licenseSource);
        return this;
    }

    @Override
    protected void doExecute(ActionListener<PutLicenseResponse> listener) {
        client.execute(PutLicenseAction.INSTANCE, request, listener);
    }
}
