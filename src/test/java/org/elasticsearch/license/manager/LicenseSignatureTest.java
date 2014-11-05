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

package org.elasticsearch.license.manager;

import org.elasticsearch.license.AbstractLicensingTestBase;
import org.elasticsearch.license.TestUtils;
import org.elasticsearch.license.core.ESLicense;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.carrotsearch.randomizedtesting.RandomizedTest.randomIntBetween;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class LicenseSignatureTest extends AbstractLicensingTestBase {

    private static ESLicenseManager esLicenseManager;

    @BeforeClass
    public static void setupManager() {
        esLicenseManager = new ESLicenseManager();
    }

    @Test
    public void testLicenseGeneration() throws Exception {
        int n = randomIntBetween(5, 15);
        List<LicenseSpec> licenseSpecs = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            licenseSpecs.add(generateRandomLicenseSpec());
        }

        Set<ESLicense> generatedLicenses = generateSignedLicenses(licenseSpecs);
        assertThat(generatedLicenses.size(), equalTo(n));

        Set<String> signatures = new HashSet<>();
        for (ESLicense license : generatedLicenses) {
            signatures.add(license.signature());
        }
        Set<ESLicense> licenseFromSignatures = esLicenseManager.fromSignatures(signatures);

        TestUtils.isSame(generatedLicenses, licenseFromSignatures);
    }
}
