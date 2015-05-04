/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated
 *  All Rights Reserved.
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
import org.elasticsearch.license.core.License;
import org.elasticsearch.license.core.LicenseVerifier;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;

public class LicenseVerificationTests extends AbstractLicensingTestBase {

    @Test
    public void testGeneratedLicenses() throws Exception {
        License shieldLicense = generateSignedLicense("shield", TimeValue.timeValueHours(2 * 24), pubKeyPath, priKeyPath);
        assertThat(LicenseVerifier.verifyLicense(shieldLicense), equalTo(true));
    }

    @Test
    public void testMultipleFeatureLicenses() throws Exception {
        License shieldLicense = generateSignedLicense("shield", TimeValue.timeValueHours(2 * 24), pubKeyPath, priKeyPath);
        License marvelLicense = generateSignedLicense("marvel", TimeValue.timeValueHours(2 * 24), pubKeyPath, priKeyPath);

        assertThat(LicenseVerifier.verifyLicenses(Arrays.asList(shieldLicense, marvelLicense)), equalTo(true));
    }

    @Test
    public void testLicenseTampering() throws Exception {
        License license = generateSignedLicense("shield", TimeValue.timeValueHours(2), pubKeyPath, priKeyPath);

        final License tamperedLicense = License.builder()
                .fromLicenseSpec(license, license.signature())
                .expiryDate(license.expiryDate() + 10 * 24 * 60 * 60 * 1000l)
                .validate()
                .build();

        assertThat(LicenseVerifier.verifyLicense(tamperedLicense), equalTo(false));
    }

    @Test
    public void testRandomLicenseVerification() throws Exception {
        int n = randomIntBetween(5, 15);
        List<TestUtils.LicenseSpec> licenseSpecs = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            licenseSpecs.add(TestUtils.generateRandomLicenseSpec());
        }

        Set<License> generatedLicenses = generateSignedLicenses(licenseSpecs, pubKeyPath, priKeyPath);
        assertThat(generatedLicenses.size(), equalTo(n));

        for (License generatedLicense: generatedLicenses) {
            assertThat(LicenseVerifier.verifyLicense(generatedLicense), equalTo(true));
        }
    }
}
