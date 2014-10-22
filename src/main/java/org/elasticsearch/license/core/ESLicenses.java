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

package org.elasticsearch.license.core;

import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.util.*;

public class ESLicenses {

    public static void toXContent(Collection<ESLicense> licenses, XContentBuilder builder) throws IOException {
        builder.startObject();
        builder.startArray("licenses");
        for (ESLicense license : licenses) {
            ESLicense.toXContent(license, builder);
        }
        builder.endArray();
        builder.endObject();
    }

    public static Set<ESLicense> fromSource(String content) throws IOException {
        return fromSource(content.getBytes(LicensesCharset.UTF_8));
    }

    public static Set<ESLicense> fromSource(byte[] bytes) throws IOException {
        return fromXContent(XContentFactory.xContent(bytes).createParser(bytes));
    }

    private static Set<ESLicense> fromXContent(XContentParser parser) throws IOException {
        Set<ESLicense> esLicenses = new HashSet<>();
        final Map<String, Object> licensesMap = parser.mapAndClose();
        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> licenseMaps = (ArrayList<Map<String, Object>>)licensesMap.get("licenses");
        for (Map<String, Object> licenseMap : licenseMaps) {
            final ESLicense esLicense = ESLicense.fromXContent(licenseMap);
            esLicenses.add(esLicense);
        }
        return esLicenses;
    }

    public static Set<ESLicense> readFrom(StreamInput in) throws IOException {
        int size = in.readVInt();
        Set<ESLicense> esLicenses = new HashSet<>(size);
        for (int i = 0; i < size; i++) {
            esLicenses.add(ESLicense.readFrom(in));
        }
        return esLicenses;
    }

    public static void writeTo(Set<ESLicense> esLicenses, StreamOutput out) throws IOException {
        out.writeVInt(esLicenses.size());
        for (ESLicense license : esLicenses) {
            ESLicense.writeTo(license, out);
        }

    }
}
