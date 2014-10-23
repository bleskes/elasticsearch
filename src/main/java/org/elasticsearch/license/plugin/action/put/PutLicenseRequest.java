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
import org.elasticsearch.license.core.ESLicense;
import org.elasticsearch.license.core.ESLicenses;

import java.io.IOException;
import java.util.List;


public class PutLicenseRequest extends AcknowledgedRequest<PutLicenseRequest> {

    private List<ESLicense> licenses;

    public PutLicenseRequest() {
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }

    /**
     * Parses licenses from json format to an instance of {@link org.elasticsearch.license.core.ESLicenses}
     * @param licenseDefinition licenses definition
     */
    public PutLicenseRequest licenses(String licenseDefinition) {
        try {
            return licenses(ESLicenses.fromSource(licenseDefinition));
        } catch (IOException e) {
            throw new ElasticsearchIllegalArgumentException("failed to parse licenses source", e);
        }
    }

    public PutLicenseRequest licenses(List<ESLicense> esLicenses) {
        this.licenses = esLicenses;
        return this;
    }

    public List<ESLicense> licenses() {
        return licenses;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        licenses = ESLicenses.readFrom(in);
        readTimeout(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        ESLicenses.writeTo(licenses, out);
        writeTimeout(out);
    }
}
