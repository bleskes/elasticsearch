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

package org.elasticsearch.shield.authc;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.authc.activedirectory.ActiveDirectoryRealm;
import org.elasticsearch.shield.authc.file.FileRealm;
import org.elasticsearch.shield.authc.ldap.LdapRealm;
import org.elasticsearch.shield.authc.pki.PkiRealm;
import org.elasticsearch.test.ESTestCase;

import static org.hamcrest.Matchers.containsString;

/**
 * Unit tests for the AuthenticationModule
 */
public class AuthenticationModuleTests extends ESTestCase {
    public void testAddingReservedRealmType() {
        Settings settings = Settings.EMPTY;
        AuthenticationModule module = new AuthenticationModule(settings);
        try {
            module.addCustomRealm(randomFrom(PkiRealm.TYPE, LdapRealm.TYPE, ActiveDirectoryRealm.TYPE, FileRealm.TYPE),
                    randomFrom(PkiRealm.Factory.class, LdapRealm.Factory.class, ActiveDirectoryRealm.Factory.class,
                            FileRealm.Factory.class));
            fail("overriding a built in realm type is not allowed!");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("cannot redefine"));
        }
    }

    public void testAddingNullOrEmptyType() {
        Settings settings = Settings.EMPTY;
        AuthenticationModule module = new AuthenticationModule(settings);
        try {
            module.addCustomRealm(randomBoolean() ? null : "",
                    randomFrom(PkiRealm.Factory.class, LdapRealm.Factory.class, ActiveDirectoryRealm.Factory.class,
                            FileRealm.Factory.class));
            fail("type must not be null");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("null or empty"));
        }
    }

    public void testAddingNullFactory() {
        Settings settings = Settings.EMPTY;
        AuthenticationModule module = new AuthenticationModule(settings);
        try {
            module.addCustomRealm(randomAsciiOfLength(7), null);
            fail("factory must not be null");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("null"));
        }
    }
}
