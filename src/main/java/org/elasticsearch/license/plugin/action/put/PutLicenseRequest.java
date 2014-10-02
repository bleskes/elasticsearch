/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.license.plugin.action.put;

import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.support.master.AcknowledgedRequest;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.license.core.ESLicenses;
import org.elasticsearch.license.core.LicenseUtils;

import java.io.IOException;

public class PutLicenseRequest extends AcknowledgedRequest<PutLicenseRequest> {

    private ESLicenses license;

    public PutLicenseRequest() {
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
        /*
        ActionRequestValidationException validationException = null;
        if (name == null) {
            validationException = addValidationError("name is missing", validationException);
        }
        if (type == null) {
            validationException = addValidationError("type is missing", validationException);
        }
        return validationException;
        */
    }

    /**
     * Parses license from json format to an instance of {@link org.elasticsearch.license.core.ESLicenses}
     * @param licenseDefinition license definition
     */
    public PutLicenseRequest license(String licenseDefinition) {
        try {
            return license(LicenseUtils.readLicensesFromString(licenseDefinition));
        } catch (IOException e) {
            throw new ElasticsearchIllegalArgumentException("failed to parse license source", e);
        }
    }

    public PutLicenseRequest license(ESLicenses esLicenses) {
        this.license = esLicenses;
        return this;
    }

    public ESLicenses license() {
        return license;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        license = LicenseUtils.readLicenseFromInputStream(in);
        readTimeout(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        LicenseUtils.dumpLicenseAsJson(license, out);
        writeTimeout(out);
    }
}
