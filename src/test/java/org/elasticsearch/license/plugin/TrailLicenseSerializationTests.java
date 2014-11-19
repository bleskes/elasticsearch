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
package org.elasticsearch.license.plugin;

import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.license.AbstractLicensingTestBase;
import org.elasticsearch.license.TestUtils;
import org.elasticsearch.license.core.License;
import org.elasticsearch.license.plugin.core.TrialLicenseUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.carrotsearch.randomizedtesting.RandomizedTest.randomIntBetween;

public class TrailLicenseSerializationTests extends AbstractLicensingTestBase {

    @Test
    public void testSerialization() throws Exception {
        final TrialLicenseUtils.TrialLicenseBuilder trialLicenseBuilder = TrialLicenseUtils.builder()
                .duration(TimeValue.timeValueHours(2))
                .maxNodes(5)
                .issuedTo("customer")
                .issueDate(System.currentTimeMillis());
        int n = randomIntBetween(2, 5);
        List<License> trialLicenses = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            trialLicenses.add(trialLicenseBuilder.feature("feature__" + String.valueOf(i)).build());
        }
        for (License trialLicense : trialLicenses) {
            String encodedTrialLicense = TrialLicenseUtils.toEncodedTrialLicense(trialLicense);
            final License fromEncodedTrialLicense = TrialLicenseUtils.fromEncodedTrialLicense(encodedTrialLicense);
            TestUtils.isSame(fromEncodedTrialLicense, trialLicense);
        }
    }
}
