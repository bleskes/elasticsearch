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

import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.support.master.AcknowledgedRequest;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.license.core.License;
import org.elasticsearch.license.core.Licenses;

import java.io.IOException;
import java.util.List;


public class PutLicenseRequest extends AcknowledgedRequest<PutLicenseRequest> {

    private List<License> licenses;

    public PutLicenseRequest() {
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }

    /**
     * Parses licenses from json format to an instance of {@link org.elasticsearch.license.core.Licenses}
     *
     * @param licenseDefinition licenses definition
     */
    public PutLicenseRequest licenses(String licenseDefinition) {
        try {
            return licenses(Licenses.fromSource(licenseDefinition));
        } catch (IOException e) {
            throw new IllegalArgumentException("failed to parse licenses source", e);
        }
    }

    public PutLicenseRequest licenses(List<License> licenses) {
        this.licenses = licenses;
        return this;
    }

    public List<License> licenses() {
        return licenses;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        licenses = Licenses.readFrom(in);
        readTimeout(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        Licenses.writeTo(licenses, out);
        writeTimeout(out);
    }
}
