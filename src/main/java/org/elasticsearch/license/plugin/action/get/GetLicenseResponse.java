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

package org.elasticsearch.license.plugin.action.get;

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.license.core.ESLicense;
import org.elasticsearch.license.core.ESLicenses;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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