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
package org.elasticsearch.license.plugin.core;

import java.util.List;

import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.license.core.License;
import org.elasticsearch.license.plugin.TestUtils;
import org.elasticsearch.license.plugin.TestUtils.AssertingLicensee;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import static org.hamcrest.Matchers.equalTo;

public class LicensesNotificationTests extends AbstractLicenseServiceTestCase {

    public void testLicenseNotification() throws Exception {
        final License license = TestUtils.generateSignedLicense(TimeValue.timeValueHours(48));
        int nLicensee = randomIntBetween(1, 3);
        AssertingLicensee[] assertingLicensees = new AssertingLicensee[nLicensee];
        for (int i = 0; i < assertingLicensees.length; i++) {
            assertingLicensees[i] = new AssertingLicensee("testLicenseNotification" + i, logger);
        }
        setInitialState(license, assertingLicensees);
        licenseService.start();
        for (int i = 0; i < assertingLicensees.length; i++) {
            assertLicenseStates(assertingLicensees[i], true);
        }
        clock.fastForward(TimeValue.timeValueMillis(license.expiryDate() - clock.millis()));
        final LicensesMetaData licensesMetaData = new LicensesMetaData(license);
        licenseService.onUpdate(licensesMetaData);
        for (AssertingLicensee assertingLicensee : assertingLicensees) {
            assertLicenseStates(assertingLicensee, true);
        }
        clock.fastForward(TimeValue.timeValueMillis((license.expiryDate() +
                LicenseService.GRACE_PERIOD_DURATION.getMillis()) - clock.millis()));
        licenseService.onUpdate(licensesMetaData);
        for (AssertingLicensee assertingLicensee : assertingLicensees) {
            assertLicenseStates(assertingLicensee, true, false);
        }
        clock.setTime(new DateTime(DateTimeZone.UTC));
        final License newLicense = TestUtils.generateSignedLicense(TimeValue.timeValueHours(2));
        clock.fastForward(TimeValue.timeValueHours(1));
        LicensesMetaData licensesMetaData1 = new LicensesMetaData(newLicense);
        licenseService.onUpdate(licensesMetaData1);
        for (AssertingLicensee assertingLicensee : assertingLicensees) {
            assertLicenseStates(assertingLicensee, true, false, true);
        }
    }

    private void assertLicenseStates(AssertingLicensee licensee, boolean... states) {
        StringBuilder msg = new StringBuilder();
        msg.append("Actual: ");
        msg.append(dumpLicensingStates(licensee.statuses));
        msg.append(" Expected: ");
        msg.append(dumpLicensingStates(states));
        assertThat(msg.toString(), licensee.statuses.size(), equalTo(states.length));
        for (int i = 0; i < states.length; i++) {
            assertThat(msg.toString(), licensee.statuses.get(i).isActive(), equalTo(states[i]));
        }
    }

    private String dumpLicensingStates(List<Licensee.Status> statuses) {
        return dumpLicensingStates(statuses.toArray(new Licensee.Status[statuses.size()]));
    }

    private String dumpLicensingStates(Licensee.Status... statuses) {
        boolean[] states = new boolean[statuses.length];
        for (int i = 0; i < statuses.length; i++) {
            states[i] = statuses[i].isActive();
        }
        return dumpLicensingStates(states);
    }

    private String dumpLicensingStates(boolean... states) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < states.length; i++) {
            sb.append(states[i]);
            if (i != states.length - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
