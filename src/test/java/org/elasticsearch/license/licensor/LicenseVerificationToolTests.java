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
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.license.AbstractLicensingTestBase;
import org.elasticsearch.license.TestUtils;
import org.elasticsearch.license.core.ESLicense;
import org.elasticsearch.license.core.ESLicenses;
import org.elasticsearch.license.licensor.tools.LicenseVerificationTool;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import static com.carrotsearch.randomizedtesting.RandomizedTest.randomIntBetween;
import static com.carrotsearch.randomizedtesting.RandomizedTest.randomRealisticUnicodeOfCodepointLengthBetween;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class LicenseVerificationToolTests extends AbstractLicensingTestBase {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testMissingCLTArgs() throws Exception {
        ESLicense singedLicense = generateSignedLicense(randomRealisticUnicodeOfCodepointLengthBetween(5, 15),
                TimeValue.timeValueHours(1));

        String[] args = new String[2];
        args[0] = "--licenssFiles";
        args[1] = dumpLicense(singedLicense);

        try {
            runLicenseVerificationTool(args);
            fail("mandatory param '--licensesFiles' should throw an exception");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("--licensesFiles"));
        }
    }

    @Test
    public void testSimple() throws Exception {
        ESLicense singedLicense = generateSignedLicense(randomRealisticUnicodeOfCodepointLengthBetween(5, 15),
                TimeValue.timeValueHours(1));

        String[] args = new String[2];
        args[0] = "--licensesFiles";
        args[1] = dumpLicense(singedLicense);

        String licenseOutput = runLicenseVerificationTool(args);
        List<ESLicense> licensesOutput = ESLicenses.fromSource(licenseOutput);

        assertThat(licensesOutput.size(), equalTo(1));

        ESLicense expectedLicense = ESLicense.builder()
                .fromLicenseSpec(singedLicense, licensesOutput.get(0).signature())
                .build();

        TestUtils.isSame(expectedLicense, licensesOutput.get(0));
    }

    @Test
    public void testWithLicenseFiles() throws Exception {
        int n = randomIntBetween(3, 10);
        Set<ESLicense> signedLicenses = new HashSet<>();
        for (int i = 0; i < n; i++) {
            signedLicenses.add(generateSignedLicense(randomRealisticUnicodeOfCodepointLengthBetween(5, 15),
                    TimeValue.timeValueHours(1)));
        }

        StringBuilder licenseFilePathString = new StringBuilder();
        ESLicense[] esLicenses = signedLicenses.toArray(new ESLicense[n]);
        for (int i = 0; i < n; i++) {
            licenseFilePathString.append(dumpLicense(esLicenses[i]));
            if (i != esLicenses.length - 1) {
                licenseFilePathString.append(":");
            }
        }

        String[] args = new String[2];
        args[0] = "--licensesFiles";
        args[1] = licenseFilePathString.toString();

        String licenseOutput = runLicenseVerificationTool(args);
        List<ESLicense> output = ESLicenses.fromSource(licenseOutput);

        assertThat(output.size(), equalTo(n));

        Set<ESLicense> licensesOutput = new HashSet<>();
        Map<String, ESLicense> expectedLicenses = ESLicenses.reduceAndMap(signedLicenses);
        for (ESLicense license : output) {
            licensesOutput.add(
                    ESLicense.builder()
                            .fromLicenseSpec(license, expectedLicenses.get(license.feature()).signature())
                            .build()
            );
        }

        TestUtils.isSame(signedLicenses, licensesOutput);

    }

    private String dumpLicense(ESLicense license) throws Exception {
        File tempFile = temporaryFolder.newFile("licenses.json");
        try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            XContentBuilder builder = XContentFactory.contentBuilder(XContentType.JSON, outputStream);
            ESLicenses.toXContent(Collections.singletonList(license), builder, ToXContent.EMPTY_PARAMS);
            builder.flush();
        }
        return tempFile.getAbsolutePath();
    }


    private String runLicenseVerificationTool(String[] args) throws IOException {
        File tempFile = temporaryFolder.newFile("licence_verification.out");
        try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            LicenseVerificationTool.run(args, outputStream);
        }
        return FileUtils.readFileToString(tempFile);
    }
}
