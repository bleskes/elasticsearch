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

package org.elasticsearch.shield.license;

import org.elasticsearch.license.core.License;
import org.elasticsearch.license.plugin.core.LicenseState;
import org.elasticsearch.license.plugin.core.Licensee;
import org.elasticsearch.test.ESTestCase;

import static org.hamcrest.Matchers.*;

/**
 * Unit tests for the {@link ShieldLicenseState}
 */
public class ShieldLicenseStateTests extends ESTestCase {

    public void testDefaults() {
        ShieldLicenseState licenseState = new ShieldLicenseState();
        assertThat(licenseState.securityEnabled(), is(true));
        assertThat(licenseState.statsAndHealthEnabled(), is(true));
        assertThat(licenseState.documentAndFieldLevelSecurityEnabled(), is(true));
    }

    public void testBasic() {
        ShieldLicenseState licenseState = new ShieldLicenseState();
        licenseState.updateStatus(new Licensee.Status(License.OperationMode.BASIC, randomBoolean() ? LicenseState.ENABLED : LicenseState.GRACE_PERIOD));

        assertThat(licenseState.securityEnabled(), is(false));
        assertThat(licenseState.statsAndHealthEnabled(), is(true));
        assertThat(licenseState.documentAndFieldLevelSecurityEnabled(), is(false));
    }

    public void testBasicExpired() {
        ShieldLicenseState licenseState = new ShieldLicenseState();
        licenseState.updateStatus(new Licensee.Status(License.OperationMode.BASIC, LicenseState.DISABLED));

        assertThat(licenseState.securityEnabled(), is(false));
        assertThat(licenseState.statsAndHealthEnabled(), is(false));
        assertThat(licenseState.documentAndFieldLevelSecurityEnabled(), is(false));
    }

    public void testGold() {
        ShieldLicenseState licenseState = new ShieldLicenseState();
        licenseState.updateStatus(new Licensee.Status(License.OperationMode.GOLD, randomBoolean() ? LicenseState.ENABLED : LicenseState.GRACE_PERIOD));

        assertThat(licenseState.securityEnabled(), is(true));
        assertThat(licenseState.statsAndHealthEnabled(), is(true));
        assertThat(licenseState.documentAndFieldLevelSecurityEnabled(), is(false));
    }

    public void testGoldExpired() {
        ShieldLicenseState licenseState = new ShieldLicenseState();
        licenseState.updateStatus(new Licensee.Status(License.OperationMode.GOLD, LicenseState.DISABLED));

        assertThat(licenseState.securityEnabled(), is(true));
        assertThat(licenseState.statsAndHealthEnabled(), is(false));
        assertThat(licenseState.documentAndFieldLevelSecurityEnabled(), is(false));
    }

    public void testPlatinum() {
        ShieldLicenseState licenseState = new ShieldLicenseState();
        licenseState.updateStatus(new Licensee.Status(License.OperationMode.PLATINUM, randomBoolean() ? LicenseState.ENABLED : LicenseState.GRACE_PERIOD));

        assertThat(licenseState.securityEnabled(), is(true));
        assertThat(licenseState.statsAndHealthEnabled(), is(true));
        assertThat(licenseState.documentAndFieldLevelSecurityEnabled(), is(true));
    }

    public void testPlatinumExpired() {
        ShieldLicenseState licenseState = new ShieldLicenseState();
        licenseState.updateStatus(new Licensee.Status(License.OperationMode.PLATINUM, LicenseState.DISABLED));

        assertThat(licenseState.securityEnabled(), is(true));
        assertThat(licenseState.statsAndHealthEnabled(), is(false));
        assertThat(licenseState.documentAndFieldLevelSecurityEnabled(), is(true));
    }
}
