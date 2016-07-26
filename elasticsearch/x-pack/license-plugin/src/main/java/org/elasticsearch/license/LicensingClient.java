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

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.license.core.License;

/**
 *
 */
public class LicensingClient {

    private final ElasticsearchClient client;

    public LicensingClient(ElasticsearchClient client) {
        this.client = client;
    }

    public PutLicenseRequestBuilder preparePutLicense(License license) {
        return new PutLicenseRequestBuilder(client).setLicense(license);
    }

    public void putLicense(PutLicenseRequest request, ActionListener<PutLicenseResponse> listener) {
        client.execute(PutLicenseAction.INSTANCE, request, listener);
    }

    public GetLicenseRequestBuilder prepareGetLicense() {
        return new GetLicenseRequestBuilder(client);
    }

    public void getLicense(GetLicenseRequest request, ActionListener<GetLicenseResponse> listener) {
        client.execute(GetLicenseAction.INSTANCE, request, listener);
    }

    public DeleteLicenseRequestBuilder prepareDeleteLicense() {
        return new DeleteLicenseRequestBuilder(client);
    }

    public void deleteLicense(DeleteLicenseRequest request, ActionListener<DeleteLicenseResponse> listener) {
        client.execute(DeleteLicenseAction.INSTANCE, request, listener);
    }
}
