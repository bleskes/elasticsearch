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

package org.elasticsearch.shield.authc.ldap;

import org.apache.lucene.util.LuceneTestCase;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.authc.RealmConfig;
import org.elasticsearch.shield.authc.support.SecuredStringTests;
import org.elasticsearch.shield.authc.support.ldap.*;
import org.elasticsearch.shield.ssl.SSLService;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.elasticsearch.test.junit.annotations.Network;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;

@Network
public class OpenLdapTests extends ElasticsearchTestCase {

    public static final String OPEN_LDAP_URL = "ldaps://54.200.235.244:636";
    public static final String PASSWORD = "NickFuryHeartsES";

    @Before
    public void initializeSslSocketFactory() throws Exception {
        Path keystore = Paths.get(OpenLdapTests.class.getResource("../support/ldap/ldaptrust.jks").toURI()).toAbsolutePath();

        /*
         * Prior to each test we reinitialize the socket factory with a new SSLService so that we get a new SSLContext.
         * If we re-use a SSLContext, previously connected sessions can get re-established which breaks hostname
         * verification tests since a re-established connection does not perform hostname verification.
         */
        AbstractLdapSslSocketFactory.init(new SSLService(ImmutableSettings.builder()
                .put("shield.ssl.keystore.path", keystore)
                .put("shield.ssl.keystore.password", "changeit")
                .build()));
    }

    @After
    public void clearSocketFactories() {
        LdapSslSocketFactory.clear();
        HostnameVerifyingLdapSslSocketFactory.clear();
    }

    @Test
    public void testConnect() {
        //openldap does not use cn as naming attributes by default

        String groupSearchBase = "ou=people,dc=oldap,dc=test,dc=elasticsearch,dc=com";
        String userTemplate = "uid={0},ou=people,dc=oldap,dc=test,dc=elasticsearch,dc=com";
        RealmConfig config = new RealmConfig("oldap-test", LdapTest.buildLdapSettings(OPEN_LDAP_URL, userTemplate, groupSearchBase, SearchScope.ONE_LEVEL));
        LdapConnectionFactory connectionFactory = new LdapConnectionFactory(config);

        String[] users = new String[] { "blackwidow", "cap", "hawkeye", "hulk", "ironman", "thor" };
        for (String user : users) {
            LdapConnection ldap = connectionFactory.open(user, SecuredStringTests.build(PASSWORD));
            assertThat(ldap.groups(), hasItem(containsString("Avengers")));
            ldap.close();
        }
    }

    @Test
    public void testGroupSearchScopeBase() {
        //base search on a groups means that the user can be in just one group

        String groupSearchBase = "cn=Avengers,ou=people,dc=oldap,dc=test,dc=elasticsearch,dc=com";
        String userTemplate = "uid={0},ou=people,dc=oldap,dc=test,dc=elasticsearch,dc=com";
        RealmConfig config = new RealmConfig("oldap-test", LdapTest.buildLdapSettings(OPEN_LDAP_URL, userTemplate, groupSearchBase, SearchScope.BASE));
        LdapConnectionFactory connectionFactory = new LdapConnectionFactory(config);

        String[] users = new String[] { "blackwidow", "cap", "hawkeye", "hulk", "ironman", "thor" };
        for (String user : users) {
            LdapConnection ldap = connectionFactory.open(user, SecuredStringTests.build(PASSWORD));
            assertThat(ldap.groups(), hasItem(containsString("Avengers")));
            ldap.close();
        }
    }

    @Test
    public void testCustomFilter() {
        String groupSearchBase = "ou=people,dc=oldap,dc=test,dc=elasticsearch,dc=com";
        String userTemplate = "uid={0},ou=people,dc=oldap,dc=test,dc=elasticsearch,dc=com";
        Settings settings = ImmutableSettings.builder()
                .put(LdapTest.buildLdapSettings(OPEN_LDAP_URL, userTemplate, groupSearchBase, SearchScope.ONE_LEVEL))
                .put("group_search.filter", "(&(objectclass=posixGroup)(memberUID={0}))")
                .put("group_search.user_attribute", "uid")
                .build();
        RealmConfig config = new RealmConfig("oldap-test", settings);
        LdapConnectionFactory connectionFactory = new LdapConnectionFactory(config);

        try (LdapConnection ldap = connectionFactory.open("selvig", SecuredStringTests.build(PASSWORD))){
            assertThat(ldap.groups(), hasItem(containsString("Geniuses")));
        }
    }

    @Test @LuceneTestCase.AwaitsFix(bugUrl = "https://github.com/elasticsearch/elasticsearch-shield/issues/499")
    public void testTcpTimeout() {
        String groupSearchBase = "ou=people,dc=oldap,dc=test,dc=elasticsearch,dc=com";
        String userTemplate = "uid={0},ou=people,dc=oldap,dc=test,dc=elasticsearch,dc=com";
        Settings settings = ImmutableSettings.builder()
                .put(LdapTest.buildLdapSettings(OPEN_LDAP_URL, userTemplate, groupSearchBase, SearchScope.ONE_LEVEL))
                .put(ConnectionFactory.HOSTNAME_VERIFICATION_SETTING, false)
                .put(ConnectionFactory.TIMEOUT_TCP_READ_SETTING, "1ms") //1 millisecond
                .build();
        RealmConfig config = new RealmConfig("oldap-test", settings);
        LdapConnectionFactory connectionFactory = new LdapConnectionFactory(config);

        try (LdapConnection ldap = connectionFactory.open("thor", SecuredStringTests.build(PASSWORD))){
            ldap.groups();
            fail("The TCP connection should timeout before getting groups back");
        } catch (LdapException e) {
            assertThat(e.getCause().getMessage(), containsString("LDAP response read timed out"));
        }
    }

    @Test
    public void testLdapTimeout() {
        String groupSearchBase = "dc=elasticsearch,dc=com";
        String userTemplate = "uid={0},ou=people,dc=oldap,dc=test,dc=elasticsearch,dc=com";
        Settings settings = ImmutableSettings.builder()
                .put(LdapTest.buildLdapSettings(OPEN_LDAP_URL, userTemplate, groupSearchBase, SearchScope.SUB_TREE))
                .put(ConnectionFactory.HOSTNAME_VERIFICATION_SETTING, false)
                .put(ConnectionFactory.TIMEOUT_LDAP_SETTING, "1ms") //1 millisecond
                .build();
        RealmConfig config = new RealmConfig("oldap-test", settings);

        LdapConnectionFactory connectionFactory = new LdapConnectionFactory(config);

        try (LdapConnection ldap = connectionFactory.open("thor", SecuredStringTests.build(PASSWORD))) {
            ldap.groups();
            fail("The server should timeout the group request");
        } catch (LdapException e) {
            assertThat(e.getCause().getMessage(), containsString("error code 32")); //openldap response for timeout
        }
    }

    @Test(expected = LdapException.class)
    public void testStandardLdapConnectionHostnameVerification() {
        //openldap does not use cn as naming attributes by default
        String groupSearchBase = "ou=people,dc=oldap,dc=test,dc=elasticsearch,dc=com";
        String userTemplate = "uid={0},ou=people,dc=oldap,dc=test,dc=elasticsearch,dc=com";
        Settings settings = ImmutableSettings.builder()
                .put(LdapTest.buildLdapSettings(OPEN_LDAP_URL, userTemplate, groupSearchBase, SearchScope.ONE_LEVEL))
                .put(LdapConnectionFactory.HOSTNAME_VERIFICATION_SETTING, true)
                .build();

        RealmConfig config = new RealmConfig("oldap-test", settings);
        LdapConnectionFactory connectionFactory = new LdapConnectionFactory(config);

        String user = "blackwidow";
        try (LdapConnection ldap = connectionFactory.open(user, SecuredStringTests.build(PASSWORD))) {
            fail("OpenLDAP certificate does not contain the correct hostname/ip so hostname verification should fail on open");
        }
    }


}
