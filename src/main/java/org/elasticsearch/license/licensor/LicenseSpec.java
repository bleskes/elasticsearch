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

package org.elasticsearch.license.licensor;

import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.license.core.DateUtils;
import org.elasticsearch.license.core.LicensesCharset;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

import static org.elasticsearch.license.core.ESLicense.SubscriptionType;
import static org.elasticsearch.license.core.ESLicense.Type;

public class LicenseSpec {
    final String uid;
    final String issuer;
    final String issuedTo;
    final long issueDate;
    final Type type;
    final SubscriptionType subscriptionType;
    final String feature;
    final long expiryDate;
    final int maxNodes;

    private LicenseSpec(String uid, String issuer, String issuedTo, long issueDate, Type type,
                        SubscriptionType subscriptionType, String feature, long expiryDate,
                        int maxNodes) {
        this.uid = uid;
        this.issuer = issuer;
        this.issuedTo = issuedTo;
        this.issueDate = issueDate;
        this.type = type;
        this.subscriptionType = subscriptionType;
        this.feature = feature;
        this.expiryDate = expiryDate;
        this.maxNodes = maxNodes;
    }


    private static class Builder {
        private String uid;
        private String issuer;
        private String issuedTo;
        private long issueDate = -1;
        private Type type;
        private SubscriptionType subscriptionType = SubscriptionType.DEFAULT;
        private String feature;
        private long expiryDate = -1;
        private int maxNodes;


        public Builder uid(String uid) {
            this.uid = uid;
            return this;
        }

        public Builder issuer(String issuer) {
            this.issuer = issuer;
            return this;
        }

        public Builder issuedTo(String issuedTo) {
            this.issuedTo = issuedTo;
            return this;
        }

        public Builder issueDate(long issueDate) {
            this.issueDate = issueDate;
            return this;
        }

        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        public Builder subscriptionType(SubscriptionType subscriptionType) {
            this.subscriptionType = subscriptionType;
            return this;
        }

        public Builder feature(String feature) {
            this.feature = feature;
            return this;
        }

        public Builder expiryDate(long expiryDate) {
            this.expiryDate = expiryDate;
            return this;
        }

        public Builder maxNodes(int maxNodes) {
            this.maxNodes = maxNodes;
            return this;
        }

        public LicenseSpec build() {
            if (uid == null) {
                uid = UUID.randomUUID().toString();
            }
            //TODO: verify params
            return new LicenseSpec(uid, issuer, issuedTo, issueDate, type, subscriptionType,
                    feature ,expiryDate, maxNodes);
        }
    }


    private static LicenseSpec fromXContent(Map<String, Object> map) throws IOException, ParseException {
        Builder builder = new Builder()
                .uid((String) map.get("uid"))
                .type(Type.fromString((String) map.get("type")))
                .subscriptionType(SubscriptionType.fromString((String) map.get("subscription_type")))
                .feature((String) map.get("feature"))
                .maxNodes((int) map.get("max_nodes"))
                .issuedTo((String) map.get("issued_to"))
                .issuer((String) map.get("issuer"));

        String issueDate = (String) map.get("issue_date");
        builder.issueDate(DateUtils.longFromDateString(issueDate));
        String expiryDate = (String) map.get("expiry_date");
        builder.expiryDate(DateUtils.longExpiryDateFromString(expiryDate));
        return builder.build();
    }

    public static Set<LicenseSpec> fromSource(String content) throws IOException, ParseException {
        return fromSource(content.getBytes(LicensesCharset.UTF_8));
    }

    public static Set<LicenseSpec> fromSource(byte[] bytes) throws IOException, ParseException {
        return fromXContents(XContentFactory.xContent(bytes).createParser(bytes));
    }

    private static Set<LicenseSpec> fromXContents(XContentParser parser) throws IOException, ParseException {
        Set<LicenseSpec> licenseSpecs = new HashSet<>();
        final Map<String, Object> licenseSpecMap = parser.mapAndClose();
        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> licenseSpecDefinitions = (ArrayList<Map<String, Object>>)licenseSpecMap.get("licenses");
        for (Map<String, Object> licenseSpecDef : licenseSpecDefinitions) {
            final LicenseSpec licenseSpec = fromXContent(licenseSpecDef);
            licenseSpecs.add(licenseSpec);
        }
        return licenseSpecs;
    }
}

