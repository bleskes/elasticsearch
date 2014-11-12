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
package org.elasticsearch.license.plugin.action.get;

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.license.core.ESLicense;
import org.elasticsearch.license.core.ESLicenses;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GetLicenseResponse extends ActionResponse {

    private List<ESLicense> licenses = new ArrayList<>();

    GetLicenseResponse() {
    }

    GetLicenseResponse(List<ESLicense> esLicenses) {
        this.licenses = esLicenses;
    }

    public List<ESLicense> licenses() {
        return licenses;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        licenses = ESLicenses.readFrom(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        ESLicenses.writeTo(licenses, out);
    }

}