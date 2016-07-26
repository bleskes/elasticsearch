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

package org.elasticsearch.xpack.security;

import org.elasticsearch.license.core.License;
import org.elasticsearch.license.core.License.OperationMode;
import org.elasticsearch.license.plugin.core.Licensee;
import org.elasticsearch.xpack.security.SecurityLicenseState.EnabledRealmType;
import org.elasticsearch.test.ESTestCase;

import static org.hamcrest.Matchers.is;

/**
 * Unit tests for the {@link SecurityLicenseState}
 */
public class SecurityLicenseStateTests extends ESTestCase {

    public void testDefaults() {
        SecurityLicenseState licenseState = new SecurityLicenseState();
        assertThat(licenseState.authenticationAndAuthorizationEnabled(), is(true));
        assertThat(licenseState.ipFilteringEnabled(), is(true));
        assertThat(licenseState.auditingEnabled(), is(true));
        assertThat(licenseState.statsAndHealthEnabled(), is(true));
        assertThat(licenseState.documentAndFieldLevelSecurityEnabled(), is(true));
        assertThat(licenseState.enabledRealmType(), is(EnabledRealmType.ALL));
    }

    public void testBasic() {
        SecurityLicenseState licenseState = new SecurityLicenseState();
        licenseState.updateStatus(new Licensee.Status(License.OperationMode.BASIC, true));

        assertThat(licenseState.authenticationAndAuthorizationEnabled(), is(false));
        assertThat(licenseState.ipFilteringEnabled(), is(false));
        assertThat(licenseState.auditingEnabled(), is(false));
        assertThat(licenseState.statsAndHealthEnabled(), is(true));
        assertThat(licenseState.documentAndFieldLevelSecurityEnabled(), is(false));
        assertThat(licenseState.enabledRealmType(), is(EnabledRealmType.NONE));
    }

    public void testBasicExpired() {
        SecurityLicenseState licenseState = new SecurityLicenseState();
        licenseState.updateStatus(new Licensee.Status(License.OperationMode.BASIC, false));

        assertThat(licenseState.authenticationAndAuthorizationEnabled(), is(false));
        assertThat(licenseState.ipFilteringEnabled(), is(false));
        assertThat(licenseState.auditingEnabled(), is(false));
        assertThat(licenseState.statsAndHealthEnabled(), is(false));
        assertThat(licenseState.documentAndFieldLevelSecurityEnabled(), is(false));
        assertThat(licenseState.enabledRealmType(), is(EnabledRealmType.NONE));
    }

    public void testStandard() {
        SecurityLicenseState licenseState = new SecurityLicenseState();
        licenseState.updateStatus(new Licensee.Status(OperationMode.STANDARD, true));

        assertThat(licenseState.authenticationAndAuthorizationEnabled(), is(true));
        assertThat(licenseState.ipFilteringEnabled(), is(false));
        assertThat(licenseState.auditingEnabled(), is(false));
        assertThat(licenseState.statsAndHealthEnabled(), is(true));
        assertThat(licenseState.documentAndFieldLevelSecurityEnabled(), is(false));
        assertThat(licenseState.enabledRealmType(), is(EnabledRealmType.NATIVE));
    }

    public void testStandardExpired() {
        SecurityLicenseState licenseState = new SecurityLicenseState();
        licenseState.updateStatus(new Licensee.Status(OperationMode.STANDARD, false));

        assertThat(licenseState.authenticationAndAuthorizationEnabled(), is(true));
        assertThat(licenseState.ipFilteringEnabled(), is(false));
        assertThat(licenseState.auditingEnabled(), is(false));
        assertThat(licenseState.statsAndHealthEnabled(), is(false));
        assertThat(licenseState.documentAndFieldLevelSecurityEnabled(), is(false));
        assertThat(licenseState.enabledRealmType(), is(EnabledRealmType.NATIVE));
    }

    public void testGold() {
        SecurityLicenseState licenseState = new SecurityLicenseState();
        licenseState.updateStatus(new Licensee.Status(License.OperationMode.GOLD, true));

        assertThat(licenseState.authenticationAndAuthorizationEnabled(), is(true));
        assertThat(licenseState.ipFilteringEnabled(), is(true));
        assertThat(licenseState.auditingEnabled(), is(true));
        assertThat(licenseState.statsAndHealthEnabled(), is(true));
        assertThat(licenseState.documentAndFieldLevelSecurityEnabled(), is(false));
        assertThat(licenseState.enabledRealmType(), is(EnabledRealmType.DEFAULT));
    }

    public void testGoldExpired() {
        SecurityLicenseState licenseState = new SecurityLicenseState();
        licenseState.updateStatus(new Licensee.Status(License.OperationMode.GOLD, false));

        assertThat(licenseState.authenticationAndAuthorizationEnabled(), is(true));
        assertThat(licenseState.ipFilteringEnabled(), is(true));
        assertThat(licenseState.auditingEnabled(), is(true));
        assertThat(licenseState.statsAndHealthEnabled(), is(false));
        assertThat(licenseState.documentAndFieldLevelSecurityEnabled(), is(false));
        assertThat(licenseState.enabledRealmType(), is(EnabledRealmType.DEFAULT));
    }

    public void testPlatinum() {
        SecurityLicenseState licenseState = new SecurityLicenseState();
        licenseState.updateStatus(new Licensee.Status(License.OperationMode.PLATINUM, true));

        assertThat(licenseState.authenticationAndAuthorizationEnabled(), is(true));
        assertThat(licenseState.ipFilteringEnabled(), is(true));
        assertThat(licenseState.auditingEnabled(), is(true));
        assertThat(licenseState.statsAndHealthEnabled(), is(true));
        assertThat(licenseState.documentAndFieldLevelSecurityEnabled(), is(true));
        assertThat(licenseState.enabledRealmType(), is(EnabledRealmType.ALL));
    }

    public void testPlatinumExpired() {
        SecurityLicenseState licenseState = new SecurityLicenseState();
        licenseState.updateStatus(new Licensee.Status(License.OperationMode.PLATINUM, false));

        assertThat(licenseState.authenticationAndAuthorizationEnabled(), is(true));
        assertThat(licenseState.ipFilteringEnabled(), is(true));
        assertThat(licenseState.auditingEnabled(), is(true));
        assertThat(licenseState.statsAndHealthEnabled(), is(false));
        assertThat(licenseState.documentAndFieldLevelSecurityEnabled(), is(true));
        assertThat(licenseState.enabledRealmType(), is(EnabledRealmType.ALL));
    }
}
