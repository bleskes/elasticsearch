/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
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

package org.elasticsearch.license.licensor;

import org.apache.commons.io.FileUtils;
import org.elasticsearch.license.AbstractLicensingTestBase;
import org.elasticsearch.license.TestUtils;
import org.elasticsearch.license.core.ESLicense;
import org.elasticsearch.license.core.ESLicenses;
import org.elasticsearch.license.licensor.tools.LicenseVerificationTool;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LicenseVerificationToolTests extends AbstractLicensingTestBase {

    @Test
    public void testEffectiveLicenseGeneration() throws Exception {
        Map<String, TestUtils.FeatureAttributes> map = new HashMap<>();
        TestUtils.FeatureAttributes featureWithLongerExpiryDate =
                new TestUtils.FeatureAttributes("shield", "subscription", "platinum", "foo bar Inc.", "elasticsearch", 10, "2014-12-13", "2015-12-13");
        map.put(TestUtils.SHIELD, featureWithLongerExpiryDate);

        String signedLicense = runLicenseGenerationTool(TestUtils.generateESLicenses(map));
        String firstLicenseFile = getAsFilePath(signedLicense);

        TestUtils.FeatureAttributes featureWithShorterExpiryDate =
                new TestUtils.FeatureAttributes("shield", "trial", "none", "foo bar Inc.", "elasticsearch", 2, "2014-12-13", "2015-01-13");
        map.put(TestUtils.SHIELD, featureWithShorterExpiryDate);

        signedLicense = runLicenseGenerationTool(TestUtils.generateESLicenses(map));
        String secondLicenseFile = getAsFilePath(signedLicense);

        String effectiveLicenseStr = runLicenseVerificationTool(new String[]{firstLicenseFile, secondLicenseFile});

        Set<ESLicense> esLicensesOutput = new HashSet<>(ESLicenses.fromSource(effectiveLicenseStr));
        map.put(TestUtils.SHIELD, featureWithLongerExpiryDate);

        // verify that the effective license strips out license for the same feature with earlier expiry dates
        TestUtils.verifyESLicenses(esLicensesOutput, map);
    }

    @Test
    public void testEffectiveLicenseForMultiFeatures() throws Exception {
        Map<String, TestUtils.FeatureAttributes> map = new HashMap<>();
        TestUtils.FeatureAttributes shieldFeatureWithLongerExpiryDate =
                new TestUtils.FeatureAttributes("shield", "subscription", "platinum", "foo bar Inc.", "elasticsearch", 10, "2014-12-13", "2015-12-13");
        map.put(TestUtils.SHIELD, shieldFeatureWithLongerExpiryDate);

        String signedLicense = runLicenseGenerationTool(TestUtils.generateESLicenses(map));
        String firstLicenseFile = getAsFilePath(signedLicense);

        TestUtils.FeatureAttributes marvelFeatureWithShorterExpiryDate =
                new TestUtils.FeatureAttributes("marvel", "trial", "none", "foo bar Inc.", "elasticsearch", 2, "2014-12-13", "2015-01-13");
        map.put(TestUtils.MARVEL, marvelFeatureWithShorterExpiryDate);

        signedLicense = runLicenseGenerationTool(TestUtils.generateESLicenses(map));
        String secondLicenseFile = getAsFilePath(signedLicense);

        String effectiveLicenseStr = runLicenseVerificationTool(new String[]{firstLicenseFile, secondLicenseFile});
        Set<ESLicense> esLicensesOutput = new HashSet<>(ESLicenses.fromSource(effectiveLicenseStr));

        // verify that the effective license contains both feature licenses
        TestUtils.verifyESLicenses(esLicensesOutput, map);
    }

    @Test
    public void testEffectiveLicenseForMultiFeatures2() throws Exception {
        Map<String, TestUtils.FeatureAttributes> map = new HashMap<>();

        TestUtils.FeatureAttributes shieldFeatureWithLongerExpiryDate =
                new TestUtils.FeatureAttributes("shield", "subscription", "platinum", "foo bar Inc.", "elasticsearch", 10, "2014-12-13", "2015-12-13");
        TestUtils.FeatureAttributes marvelFeatureWithShorterExpiryDate =
                new TestUtils.FeatureAttributes("marvel", "trial", "none", "foo bar Inc.", "elasticsearch", 2, "2014-12-13", "2015-01-13");

        map.put(TestUtils.SHIELD, shieldFeatureWithLongerExpiryDate);
        map.put(TestUtils.MARVEL, marvelFeatureWithShorterExpiryDate);

        String signedLicense = runLicenseGenerationTool(TestUtils.generateESLicenses(map));
        String firstLicenseFile = getAsFilePath(signedLicense);

        TestUtils.FeatureAttributes shieldFeatureWithShorterExpiryDate =
                new TestUtils.FeatureAttributes("shield", "subscription", "platinum", "foo bar Inc.", "elasticsearch", 10, "2014-12-13", "2015-11-13");
        TestUtils.FeatureAttributes marvelFeatureWithLongerExpiryDate =
                new TestUtils.FeatureAttributes("marvel", "trial", "none", "foo bar Inc.", "elasticsearch", 2, "2014-12-13", "2015-11-13");

        map.put(TestUtils.SHIELD, shieldFeatureWithShorterExpiryDate);
        map.put(TestUtils.MARVEL, marvelFeatureWithLongerExpiryDate);

        signedLicense = runLicenseGenerationTool(TestUtils.generateESLicenses(map));
        String secondLicenseFile = getAsFilePath(signedLicense);

        String effectiveLicenseStr = runLicenseVerificationTool(new String[]{firstLicenseFile, secondLicenseFile});
        Set<ESLicense> esLicensesOutput = new HashSet<>(ESLicenses.fromSource(effectiveLicenseStr));

        map.put(TestUtils.SHIELD, shieldFeatureWithLongerExpiryDate);
        map.put(TestUtils.MARVEL, marvelFeatureWithLongerExpiryDate);

        // verify that the generated effective license is generated from choosing individual licences from multiple files
        TestUtils.verifyESLicenses(esLicensesOutput, map);
    }

    public static String runLicenseVerificationTool(String[] licenseFiles) throws IOException {
        StringBuilder licenseFilePathString = new StringBuilder();
        for (int i = 0; i < licenseFiles.length; i++) {
            licenseFilePathString.append(licenseFiles[i]);
            if (i != licenseFiles.length - 1) {
                licenseFilePathString.append(":");
            }
        }
        String[] args = new String[4];
        args[0] = "--licensesFiles";
        args[1] = licenseFilePathString.toString();
        args[2] = "--publicKeyPath";
        args[3] = pubKeyPath;
        File temp = File.createTempFile("temp", ".out");
        temp.deleteOnExit();
        try (FileOutputStream outputStream = new FileOutputStream(temp)) {
            LicenseVerificationTool.run(args, outputStream);
        }
        return FileUtils.readFileToString(temp);
    }

    public String runLicenseGenerationTool(String licenseInput) throws IOException, ParseException {
        return TestUtils.runLicenseGenerationTool(licenseInput, pubKeyPath, priKeyPath);
    }

    private static String getAsFilePath(String content) throws IOException {
        File temp = File.createTempFile("license", ".out");
        temp.deleteOnExit();
        FileUtils.write(temp, content);
        String tempFilePath = temp.getAbsolutePath();
        while (tempFilePath.contains(":")) {
            assert temp.delete();
            tempFilePath = getAsFilePath(content);
        }
        return tempFilePath;
    }

}
