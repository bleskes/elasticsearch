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

import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.license.DateUtils;
import org.elasticsearch.license.License;
import org.elasticsearch.license.LicenseVerifier;
import org.elasticsearch.test.ESTestCase;
import org.junit.After;
import org.junit.Before;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.equalTo;

public class LicenseVerificationTests extends ESTestCase {
    protected Path pubKeyPath = null;
    protected Path priKeyPath = null;

    @Before
    public void setup() throws Exception {
        pubKeyPath = getDataPath("/public.key");
        priKeyPath = getDataPath("/private.key");
    }

    @After
    public void cleanUp() {
        pubKeyPath = null;
        priKeyPath = null;
    }

    public void testGeneratedLicenses() throws Exception {
        assertThat(LicenseVerifier.verifyLicense(TestUtils.generateSignedLicense(TimeValue.timeValueHours(2 * 24), pubKeyPath, priKeyPath),
                Files.readAllBytes(pubKeyPath)), equalTo(true));
    }

    public void testLicenseTampering() throws Exception {
        License license = TestUtils.generateSignedLicense(TimeValue.timeValueHours(2), pubKeyPath, priKeyPath);

        final License tamperedLicense = License.builder()
                .fromLicenseSpec(license, license.signature())
                .expiryDate(license.expiryDate() + 10 * 24 * 60 * 60 * 1000L)
                .validate()
                .build();

        assertThat(LicenseVerifier.verifyLicense(tamperedLicense, Files.readAllBytes(pubKeyPath)), equalTo(false));
    }

    public void testRandomLicenseVerification() throws Exception {
        TestUtils.LicenseSpec licenseSpec = TestUtils.generateRandomLicenseSpec(
                randomIntBetween(License.VERSION_START, License.VERSION_CURRENT));
        License generatedLicense = generateSignedLicense(licenseSpec, pubKeyPath, priKeyPath);
        assertThat(LicenseVerifier.verifyLicense(generatedLicense, Files.readAllBytes(pubKeyPath)), equalTo(true));
    }

    private static License generateSignedLicense(TestUtils.LicenseSpec spec, Path pubKeyPath, Path priKeyPath) throws Exception {
        LicenseSigner signer = new LicenseSigner(priKeyPath, pubKeyPath);
        License.Builder builder = License.builder()
                .uid(spec.uid)
                .feature(spec.feature)
                .type(spec.type)
                .subscriptionType(spec.subscriptionType)
                .issuedTo(spec.issuedTo)
                .issuer(spec.issuer)
                .maxNodes(spec.maxNodes);

        if (spec.expiryDate != null) {
            builder.expiryDate(DateUtils.endOfTheDay(spec.expiryDate));
        } else {
            builder.expiryDate(spec.expiryDateInMillis);
        }
        if (spec.issueDate != null) {
            builder.issueDate(DateUtils.beginningOfTheDay(spec.issueDate));
        } else {
            builder.issueDate(spec.issueDateInMillis);
        }
        builder.version(spec.version);
        return signer.sign(builder.build());
    }
}
