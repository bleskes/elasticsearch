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

import org.elasticsearch.license.AbstractLicensingTestBase;
import org.elasticsearch.license.core.DateUtils;
import org.elasticsearch.license.core.ESLicense;
import org.elasticsearch.license.core.ESLicenses;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.carrotsearch.randomizedtesting.RandomizedTest.randomIntBetween;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class LicenseSerializationTests extends AbstractLicensingTestBase {

    @Test
    public void testSimpleIssueExpiryDate() throws Exception {
        long now = System.currentTimeMillis();
        String issueDate = dateMathString("now", now);
        String expiryDate = dateMathString("now+10d/d", now);
        String licenseSpecs = generateESLicenseSpecString(Arrays.asList(new LicenseSpec("shield", issueDate, expiryDate)));
        Set<ESLicense> esLicensesOutput = new HashSet<>(ESLicenses.fromSource(licenseSpecs.getBytes(StandardCharsets.UTF_8), false));
        ESLicense generatedLicense = esLicensesOutput.iterator().next();

        assertThat(esLicensesOutput.size(), equalTo(1));
        assertThat(generatedLicense.issueDate(), equalTo(DateUtils.beginningOfTheDay(issueDate)));
        assertThat(generatedLicense.expiryDate(), equalTo(DateUtils.endOfTheDay(expiryDate)));
    }

    @Test
    public void testMultipleIssueExpiryDate() throws Exception {
        long now = System.currentTimeMillis();
        String shieldIssueDate = dateMathString("now", now);
        String shieldExpiryDate = dateMathString("now+30d/d", now);
        String marvelIssueDate = dateMathString("now", now);
        String marvelExpiryDate = dateMathString("now+60d/d", now);
        String licenseSpecs = generateESLicenseSpecString(Arrays.asList(new LicenseSpec("shield", shieldIssueDate, shieldExpiryDate)));
        String licenseSpecs1 = generateESLicenseSpecString(Arrays.asList(new LicenseSpec("marvel", marvelIssueDate, marvelExpiryDate)));
        Set<ESLicense> esLicensesOutput = new HashSet<>();
        esLicensesOutput.addAll(ESLicenses.fromSource(licenseSpecs.getBytes(StandardCharsets.UTF_8), false));
        esLicensesOutput.addAll(ESLicenses.fromSource(licenseSpecs1.getBytes(StandardCharsets.UTF_8), false));
        assertThat(esLicensesOutput.size(), equalTo(2));
        for (ESLicense esLicense : esLicensesOutput) {
            assertThat(esLicense.issueDate(), equalTo(DateUtils.beginningOfTheDay((esLicense.feature().equals("shield")) ? shieldIssueDate : marvelIssueDate)));
            assertThat(esLicense.expiryDate(), equalTo(DateUtils.endOfTheDay((esLicense.feature().equals("shield")) ? shieldExpiryDate : marvelExpiryDate)));
        }
    }

    @Test
    public void testLicensesFields() throws Exception {
        Map<String, LicenseSpec> licenseSpecs = new HashMap<>();
        for (int i = 0; i < randomIntBetween(1, 5); i++) {
            LicenseSpec randomLicenseSpec = generateRandomLicenseSpec();
            licenseSpecs.put(randomLicenseSpec.feature, randomLicenseSpec);
        }

        ArrayList<LicenseSpec> specs = new ArrayList<>(licenseSpecs.values());
        String licenseSpecsSource = generateESLicenseSpecString(specs);
        Set<ESLicense> esLicensesOutput = new HashSet<>(ESLicenses.fromSource(licenseSpecsSource.getBytes(StandardCharsets.UTF_8), false));
        assertThat(esLicensesOutput.size(), equalTo(licenseSpecs.size()));

        for (ESLicense license : esLicensesOutput) {
            LicenseSpec spec = licenseSpecs.get(license.feature());
            assertThat(spec, notNullValue());

            assertThat(license.uid(), equalTo(spec.uid));
            assertThat(license.feature(), equalTo(spec.feature));
            assertThat(license.issuedTo(), equalTo(spec.issuedTo));
            assertThat(license.issuer(), equalTo(spec.issuer));
            assertThat(license.type(), equalTo(spec.type));
            assertThat(license.subscriptionType(), equalTo(spec.subscriptionType));
            assertThat(license.maxNodes(), equalTo(spec.maxNodes));
            assertThat(license.issueDate(), equalTo(DateUtils.beginningOfTheDay(spec.issueDate)));
            assertThat(license.expiryDate(), equalTo(DateUtils.endOfTheDay(spec.expiryDate)));
        }

    }


}
