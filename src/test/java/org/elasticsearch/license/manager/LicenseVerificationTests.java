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

package org.elasticsearch.license.manager;

import net.nicholaswilliams.java.licensing.exception.InvalidLicenseException;
import org.elasticsearch.license.TestUtils;
import org.elasticsearch.license.core.DateUtils;
import org.elasticsearch.license.core.ESLicenses;
import org.elasticsearch.license.core.LicenseBuilders;
import org.elasticsearch.license.licensor.tools.KeyPairGeneratorTool;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.elasticsearch.license.core.ESLicenses.FeatureType;
import static org.elasticsearch.license.core.LicenseUtils.readLicensesFromString;
import static org.junit.Assert.*;

@Ignore("Enable once maven is setup properly; now it throws invalid signature error for all the tests when the tests always pass in intellij")
public class LicenseVerificationTests {

    private static String pubKeyPath = null;
    private static String priKeyPath = null;
    private static String keyPass = null;

    @BeforeClass
    public static void setup() throws IOException {

        // Generate temp KeyPair spec
        File privateKeyFile = File.createTempFile("privateKey", ".key");
        File publicKeyFile = File.createTempFile("publicKey", ".key");
        LicenseVerificationTests.pubKeyPath = publicKeyFile.getAbsolutePath();
        LicenseVerificationTests.priKeyPath = privateKeyFile.getAbsolutePath();
        assert privateKeyFile.delete();
        assert publicKeyFile.delete();
        LicenseVerificationTests.keyPass = "password";

        // Generate keyPair
        String[] args = new String[6];
        args[0] = "--publicKeyPath";
        args[1] = LicenseVerificationTests.pubKeyPath;
        args[2] = "--privateKeyPath";
        args[3] = LicenseVerificationTests.priKeyPath;
        args[4] = "--keyPass";
        args[5] = LicenseVerificationTests.keyPass;
        KeyPairGeneratorTool.main(args);
    }

    @Test
    public void testGeneratedLicenses() throws Exception {
        Date issueDate = new Date();
        String issueDateStr = DateUtils.dateStringFromLongDate(issueDate.getTime());
        String expiryDateStr = DateUtils.dateStringFromLongDate(DateUtils.longExpiryDateFromDate(issueDate.getTime() + 24 * 60 * 60l));
        Map<FeatureType, TestUtils.FeatureAttributes> map = new HashMap<>();
        TestUtils.FeatureAttributes featureAttributes =
                new TestUtils.FeatureAttributes("shield", "subscription", "platinum", "foo bar Inc.", "elasticsearch", 2, issueDateStr, expiryDateStr);
        map.put(FeatureType.SHIELD, featureAttributes);

        String licenseString = TestUtils.generateESLicenses(map);

        String[] args = new String[8];
        args[0] = "--license";
        args[1] = licenseString;
        args[2] = "--publicKeyPath";
        args[3] = pubKeyPath;
        args[4] = "--privateKeyPath";
        args[5] = priKeyPath;
        args[6] = "--keyPass";
        args[7] = keyPass;

        String licenseOutput = TestUtils.runLicenseGenerationTool(args);

        ESLicenses esLicensesOutput = readLicensesFromString(licenseOutput);

        ESLicenseManager esLicenseManager = new ESLicenseManager(esLicensesOutput, pubKeyPath, keyPass);

        esLicenseManager.verifyLicenses();

        verifyLicenseManager(esLicenseManager, map);
    }

    @Test
    public void testMultipleFeatureLicenses() throws Exception {
        Date issueDate = new Date();
        String issueDateStr = DateUtils.dateStringFromLongDate(issueDate.getTime());
        String expiryDateStr = DateUtils.dateStringFromLongDate(DateUtils.longExpiryDateFromDate(issueDate.getTime() + 24 * 60 * 60 * 1000l));

        Map<FeatureType, TestUtils.FeatureAttributes> map = new HashMap<>();
        TestUtils.FeatureAttributes shildFeatureAttributes =
                new TestUtils.FeatureAttributes("shield", "trial", "none", "foo bar Inc.", "elasticsearch", 2, issueDateStr, expiryDateStr);
        TestUtils.FeatureAttributes marvelFeatureAttributes =
                new TestUtils.FeatureAttributes("marvel", "subscription", "silver", "foo1 bar Inc.", "elasticsearc3h", 10, issueDateStr, expiryDateStr);
        map.put(FeatureType.SHIELD, shildFeatureAttributes);
        map.put(FeatureType.MARVEL, marvelFeatureAttributes);

        String licenseString = TestUtils.generateESLicenses(map);

        String[] args = new String[8];
        args[0] = "--license";
        args[1] = licenseString;
        args[2] = "--publicKeyPath";
        args[3] = pubKeyPath;
        args[4] = "--privateKeyPath";
        args[5] = priKeyPath;
        args[6] = "--keyPass";
        args[7] = keyPass;

        String licenseOutput = TestUtils.runLicenseGenerationTool(args);

        ESLicenses esLicensesOutput = readLicensesFromString(licenseOutput);

        ESLicenseManager esLicenseManager = new ESLicenseManager(esLicensesOutput, pubKeyPath, keyPass);

        esLicenseManager.verifyLicenses();

        verifyLicenseManager(esLicenseManager, map);
    }

    @Test
    public void testLicenseExpiry() throws Exception {

        Date issueDate = new Date();
        String issueDateStr = DateUtils.dateStringFromLongDate(issueDate.getTime());
        String expiryDateStr = DateUtils.dateStringFromLongDate(DateUtils.longExpiryDateFromDate(issueDate.getTime() + 24 * 60 * 60l));

        String expiredExpiryDateStr = DateUtils.dateStringFromLongDate(DateUtils.longExpiryDateFromDate(issueDate.getTime() - 5 * 24 * 60 * 60 * 1000l));

        Map<FeatureType, TestUtils.FeatureAttributes> map = new HashMap<>();
        TestUtils.FeatureAttributes shildFeatureAttributes =
                new TestUtils.FeatureAttributes("shield", "trial", "none", "foo bar Inc.", "elasticsearch", 2, issueDateStr, expiryDateStr);
        TestUtils.FeatureAttributes marvelFeatureAttributes =
                new TestUtils.FeatureAttributes("marvel", "subscription", "silver", "foo1 bar Inc.", "elasticsearc3h", 10, issueDateStr, expiredExpiryDateStr);
        map.put(FeatureType.SHIELD, shildFeatureAttributes);
        map.put(FeatureType.MARVEL, marvelFeatureAttributes);

        String licenseString = TestUtils.generateESLicenses(map);

        String[] args = new String[8];
        args[0] = "--license";
        args[1] = licenseString;
        args[2] = "--publicKeyPath";
        args[3] = pubKeyPath;
        args[4] = "--privateKeyPath";
        args[5] = priKeyPath;
        args[6] = "--keyPass";
        args[7] = keyPass;

        String licenseOutput = TestUtils.runLicenseGenerationTool(args);

        ESLicenses esLicensesOutput = readLicensesFromString(licenseOutput);

        ESLicenseManager esLicenseManager = new ESLicenseManager(esLicensesOutput, pubKeyPath, keyPass);

        // All validation for shield license should be normal as expected
        verifyLicenseManager(esLicenseManager, Collections.singletonMap(FeatureType.SHIELD, shildFeatureAttributes));

        assertFalse("license for marvel should not be valid due to expired expiry date", esLicenseManager.hasLicenseForFeature(FeatureType.MARVEL));
    }

    @Test
    public void testLicenseTampering() throws Exception {

        Date issueDate = new Date();
        String issueDateStr = DateUtils.dateStringFromLongDate(issueDate.getTime());
        String expiryDateStr = DateUtils.dateStringFromLongDate(DateUtils.longExpiryDateFromDate(issueDate.getTime() + 24 * 60 * 60l));
        Map<FeatureType, TestUtils.FeatureAttributes> map = new HashMap<>();
        TestUtils.FeatureAttributes featureAttributes =
                new TestUtils.FeatureAttributes("shield", "subscription", "platinum", "foo bar Inc.", "elasticsearch", 2, issueDateStr, expiryDateStr);
        map.put(FeatureType.SHIELD, featureAttributes);

        String licenseString = TestUtils.generateESLicenses(map);

        String[] args = new String[8];
        args[0] = "--license";
        args[1] = licenseString;
        args[2] = "--publicKeyPath";
        args[3] = pubKeyPath;
        args[4] = "--privateKeyPath";
        args[5] = priKeyPath;
        args[6] = "--keyPass";
        args[7] = keyPass;

        String licenseOutput = TestUtils.runLicenseGenerationTool(args);

        ESLicenses esLicensesOutput = readLicensesFromString(licenseOutput);

        ESLicenses.ESLicense esLicense = esLicensesOutput.get(FeatureType.SHIELD);

        long originalExpiryDate = esLicense.expiryDate();
        final ESLicenses.ESLicense tamperedLicense = LicenseBuilders.licenseBuilder(true)
                .fromLicense(esLicense)
                .expiryDate(esLicense.expiryDate() + 10 * 24 * 60 * 60 * 1000l)
                .feature(FeatureType.SHIELD)
                .issuer("elasticsqearch")
                .build();

        ESLicenses tamperedLicenses = LicenseBuilders.licensesBuilder().license(tamperedLicense).build();

        ESLicenseManager esLicenseManager = null;
        try {
            esLicenseManager = new ESLicenseManager(tamperedLicenses, pubKeyPath, keyPass);
            assertTrue("License manager should always report the original (signed) expiry date", esLicenseManager.getExpiryDateForLicense(FeatureType.SHIELD) == originalExpiryDate);
            esLicenseManager.verifyLicenses();
            fail();
        } catch (InvalidLicenseException e) {
            assertTrue("Exception should contain 'Invalid License' ", e.getMessage().contains("Invalid License"));
        }
    }

    public static void verifyLicenseManager(ESLicenseManager esLicenseManager, Map<ESLicenses.FeatureType, TestUtils.FeatureAttributes> featureAttributeMap) throws ParseException {

        for (Map.Entry<FeatureType, TestUtils.FeatureAttributes> entry : featureAttributeMap.entrySet()) {
            TestUtils.FeatureAttributes featureAttributes = entry.getValue();
            FeatureType featureType = entry.getKey();
            assertTrue("License should have issuedTo of " + featureAttributes.issuedTo, esLicenseManager.getIssuedToForLicense(featureType).equals(featureAttributes.issuedTo));
            assertTrue("License should have issuer of " + featureAttributes.issuer, esLicenseManager.getIssuerForLicense(featureType).equals(featureAttributes.issuer));
            assertTrue("License should have issue date of " + DateUtils.longFromDateString(featureAttributes.issueDate), esLicenseManager.getIssueDateForLicense(featureType) == DateUtils.longFromDateString(featureAttributes.issueDate));
            assertTrue("License should have expiry date of " + DateUtils.longExpiryDateFromString(featureAttributes.expiryDate), esLicenseManager.getExpiryDateForLicense(featureType) == DateUtils.longExpiryDateFromString(featureAttributes.expiryDate));
            assertTrue("License should have type of " + featureAttributes.featureType, esLicenseManager.getTypeForLicense(featureType) == ESLicenses.Type.fromString(featureAttributes.type));
            assertTrue("License should have subscription type of " + featureAttributes.subscriptionType, esLicenseManager.getSubscriptionTypeForLicense(featureType) == ESLicenses.SubscriptionType.fromString(featureAttributes.subscriptionType));


            assertTrue("License should be valid for shield", esLicenseManager.hasLicenseForFeature(featureType));
            assertTrue("License should be valid for maxNodes = " + (featureAttributes.maxNodes - 1), esLicenseManager.hasLicenseForNodes(featureType, featureAttributes.maxNodes - 1));
            assertTrue("License should be valid for maxNodes = " + (featureAttributes.maxNodes), esLicenseManager.hasLicenseForNodes(featureType, featureAttributes.maxNodes));
            assertFalse("License should not be valid for maxNodes = " + (featureAttributes.maxNodes + 1), esLicenseManager.hasLicenseForNodes(featureType, featureAttributes.maxNodes + 1));
        }
    }
}
