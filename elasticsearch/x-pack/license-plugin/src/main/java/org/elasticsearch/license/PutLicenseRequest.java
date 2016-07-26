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

import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.ValidateActions;
import org.elasticsearch.action.support.master.AcknowledgedRequest;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.license.core.License;

import java.io.IOException;


public class PutLicenseRequest extends AcknowledgedRequest<PutLicenseRequest> {

    private License license;
    private boolean acknowledge = false;

    public PutLicenseRequest() {
    }

    @Override
    public ActionRequestValidationException validate() {
        return (license == null) ? ValidateActions.addValidationError("license is missing", null) : null;
    }

    /**
     * Parses license from json format to an instance of {@link org.elasticsearch.license.core.License}
     *
     * @param licenseDefinition licenses definition
     */
    public PutLicenseRequest license(String licenseDefinition) {
        try {
            return license(License.fromSource(licenseDefinition));
        } catch (IOException e) {
            throw new IllegalArgumentException("failed to parse license source", e);
        }
    }

    public PutLicenseRequest license(License license) {
        this.license = license;
        return this;
    }

    public License license() {
        return license;
    }

    public PutLicenseRequest acknowledge(boolean acknowledge) {
        this.acknowledge = acknowledge;
        return this;
    }

    public boolean acknowledged() {
        return acknowledge;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        license = License.readLicense(in);
        acknowledge = in.readBoolean();
        readTimeout(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        license.writeTo(out);
        out.writeBoolean(acknowledge);
        writeTimeout(out);
    }
}
