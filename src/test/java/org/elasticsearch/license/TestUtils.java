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

package org.elasticsearch.license;

import org.apache.commons.io.FileUtils;
import org.elasticsearch.license.core.DateUtils;
import org.elasticsearch.license.core.ESLicenses;
import org.elasticsearch.license.licensor.tools.LicenseGeneratorTool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class TestUtils {


    public static String generateESLicenses(Map<ESLicenses.FeatureType, FeatureAttributes> featureAttributes) {
        StringBuilder licenseBuilder = new StringBuilder();
        int size = featureAttributes.values().size();
        int i = 0;
        for (FeatureAttributes attributes : featureAttributes.values()) {
            licenseBuilder.append("{\n" +
                    "    \"type\" : \"" + attributes.type + "\",\n" +
                    "    \"subscription_type\" : \"" + attributes.subscriptionType + "\",\n" +
                    "    \"issued_to\" : \"" + attributes.issuedTo + "\",\n" +
                    "    \"issuer\" : \"" + attributes.issuer + "\",\n" +
                    "    \"issue_date\" : \"" + attributes.issueDate + "\",\n" +
                    "    \"expiry_date\" : \"" + attributes.expiryDate + "\",\n" +
                    "    \"feature\" : \"" + attributes.featureType + "\",\n" +
                    "    \"max_nodes\" : " + attributes.maxNodes +
                    "}");
            if (++i < size) {
                licenseBuilder.append(",\n");
            }
        }
        return "{\n" +
                "  \"licenses\" : [" +
                licenseBuilder.toString() +
                "]\n" +
                "}";

    }

    public static String runLicenseGenerationTool(String licenseInput, String pubKeyPath, String priKeyPath) throws IOException {
        String args[] = new String[6];
        args[0] = "--license";
        args[1] = licenseInput;
        args[2] = "--publicKeyPath";
        args[3] = pubKeyPath;
        args[4] = "--privateKeyPath";
        args[5] = priKeyPath;

        return runLicenseGenerationTool(args);
    }

    public static String runLicenseGenerationTool(String[] args) throws IOException {
        File temp = File.createTempFile("temp", ".out");
        temp.deleteOnExit();
        try (FileOutputStream outputStream = new FileOutputStream(temp)) {
            LicenseGeneratorTool.run(args, outputStream);
        }
        return FileUtils.readFileToString(temp);
    }

    public static void verifyESLicenses(ESLicenses esLicenses, Map<ESLicenses.FeatureType, FeatureAttributes> featureAttributes) throws ParseException {
        assertTrue("Number of feature licenses should be " + featureAttributes.size(), esLicenses.features().size() == featureAttributes.size());
        for (Map.Entry<ESLicenses.FeatureType, FeatureAttributes> featureAttrTuple : featureAttributes.entrySet()) {
            ESLicenses.FeatureType featureType = featureAttrTuple.getKey();
            FeatureAttributes attributes = featureAttrTuple.getValue();
            final ESLicenses.ESLicense esLicense = esLicenses.get(featureType);
            assertTrue("license for " + featureType.string() + " should be present", esLicense != null);
            assertTrue("expected value for issuedTo was: " + attributes.issuedTo + " but got: " + esLicense.issuedTo(), esLicense.issuedTo().equals(attributes.issuedTo));
            assertTrue("expected value for type was: " + attributes.type + " but got: " + esLicense.type().string(), esLicense.type().string().equals(attributes.type));
            assertTrue("expected value for subscriptionType was: " + attributes.subscriptionType + " but got: " + esLicense.subscriptionType().string(), esLicense.subscriptionType().string().equals(attributes.subscriptionType));
            assertTrue("expected value for feature was: " + attributes.featureType + " but got: " + esLicense.feature().string(), esLicense.feature().string().equals(attributes.featureType));
            assertTrue("expected value for issueDate was: " + DateUtils.longFromDateString(attributes.issueDate) + " but got: " + esLicense.issueDate(), esLicense.issueDate() == DateUtils.longFromDateString(attributes.issueDate));
            assertTrue("expected value for expiryDate: " + DateUtils.longExpiryDateFromString(attributes.expiryDate) + " but got: " + esLicense.expiryDate(), esLicense.expiryDate() == DateUtils.longExpiryDateFromString(attributes.expiryDate));
            assertTrue("expected value for maxNodes: " + attributes.maxNodes + " but got: " + esLicense.maxNodes(), esLicense.maxNodes() == attributes.maxNodes);

            assertTrue("generated licenses should have non-null uid field", esLicense.uid() != null);
            assertTrue("generated licenses should have non-null signature field", esLicense.signature() != null);
        }
    }

    public static class FeatureAttributes {

        public final String featureType;
        public final String type;
        public final String subscriptionType;
        public final String issuedTo;
        public final int maxNodes;
        public final String issueDate;
        public final String expiryDate;
        public final String issuer;

        public FeatureAttributes(String featureType, String type, String subscriptionType, String issuedTo, String issuer, int maxNodes, String issueDateStr, String expiryDateStr) throws ParseException {
            this.featureType = featureType;
            this.type = type;
            this.subscriptionType = subscriptionType;
            this.issuedTo = issuedTo;
            this.issuer = issuer;
            this.maxNodes = maxNodes;
            this.issueDate = issueDateStr;
            this.expiryDate = expiryDateStr;
        }
    }
}
