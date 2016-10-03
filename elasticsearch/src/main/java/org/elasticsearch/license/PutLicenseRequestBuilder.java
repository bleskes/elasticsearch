/*
 * ELASTICSEARCH CONFIDENTIAL
 *  __________________
 *
 * [2014] Elasticsearch Incorporated. All Rights Reserved.
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
package org.elasticsearch.license;

import org.elasticsearch.action.support.master.AcknowledgedRequestBuilder;
import org.elasticsearch.client.ElasticsearchClient;

/**
 * Register license request builder
 */
public class PutLicenseRequestBuilder extends AcknowledgedRequestBuilder<PutLicenseRequest, PutLicenseResponse, PutLicenseRequestBuilder> {

    public PutLicenseRequestBuilder(ElasticsearchClient client) {
        this(client, PutLicenseAction.INSTANCE);
    }

    /**
     * Constructs register license request
     *
     * @param client elasticsearch client
     */
    public PutLicenseRequestBuilder(ElasticsearchClient client, PutLicenseAction action) {
        super(client, action, new PutLicenseRequest());
    }

    /**
     * Sets the license
     *
     * @param license license
     * @return this builder
     */
    public PutLicenseRequestBuilder setLicense(License license) {
        request.license(license);
        return this;
    }

    public PutLicenseRequestBuilder setLicense(String licenseSource) {
        request.license(licenseSource);
        return this;
    }

    public PutLicenseRequestBuilder setAcknowledge(boolean acknowledge) {
        request.acknowledge(acknowledge);
        return this;
    }
}
