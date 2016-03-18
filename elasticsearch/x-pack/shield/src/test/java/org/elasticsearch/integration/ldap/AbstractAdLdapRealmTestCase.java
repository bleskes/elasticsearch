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

package org.elasticsearch.integration.ldap;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchSecurityException;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.authc.activedirectory.ActiveDirectoryRealm;
import org.elasticsearch.shield.authc.ldap.LdapRealm;
import org.elasticsearch.shield.authc.support.SecuredString;
import org.elasticsearch.shield.authc.support.UsernamePasswordToken;
import org.elasticsearch.shield.transport.netty.ShieldNettyTransport;
import org.elasticsearch.test.ShieldIntegTestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.elasticsearch.common.settings.Settings.settingsBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.shield.authc.ldap.support.LdapSearchScope.ONE_LEVEL;
import static org.elasticsearch.shield.authc.ldap.support.LdapSearchScope.SUB_TREE;
import static org.elasticsearch.shield.authc.support.UsernamePasswordToken.BASIC_AUTH_HEADER;
import static org.elasticsearch.shield.test.ShieldTestUtils.writeFile;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;


/**
 * This test assumes all subclass tests will be of type SUITE.  It picks a random realm configuration for the tests, and
 * writes a group to role mapping file for each node.
 */
abstract public class AbstractAdLdapRealmTestCase extends ShieldIntegTestCase {

    public static final String SHIELD_AUTHC_REALMS_EXTERNAL = "shield.authc.realms.external";
    public static final String PASSWORD = "NickFuryHeartsES";
    public static final String ASGARDIAN_INDEX = "gods";
    public static final String PHILANTHROPISTS_INDEX = "philanthropists";
    public static final String SHIELD_INDEX = "shield";
    private static final String AD_ROLE_MAPPING =
            "SHIELD:  [ \"CN=SHIELD,CN=Users,DC=ad,DC=test,DC=elasticsearch,DC=com\" ] \n" +
                    "Avengers:  [ \"CN=Avengers,CN=Users,DC=ad,DC=test,DC=elasticsearch,DC=com\" ] \n" +
                    "Gods:  [ \"CN=Gods,CN=Users,DC=ad,DC=test,DC=elasticsearch,DC=com\" ] \n" +
                    "Philanthropists:  [ \"CN=Philanthropists,CN=Users,DC=ad,DC=test,DC=elasticsearch,DC=com\" ] \n";
    private static final String OLDAP_ROLE_MAPPING =
            "SHIELD: [ \"cn=SHIELD,ou=people,dc=oldap,dc=test,dc=elasticsearch,dc=com\" ] \n" +
                    "Avengers: [ \"cn=Avengers,ou=people,dc=oldap,dc=test,dc=elasticsearch,dc=com\" ] \n" +
                    "Gods: [ \"cn=Gods,ou=people,dc=oldap,dc=test,dc=elasticsearch,dc=com\" ] \n" +
                    "Philanthropists: [ \"cn=Philanthropists,ou=people,dc=oldap,dc=test,dc=elasticsearch,dc=com\" ] \n";

    static protected RealmConfig realmConfig;

    @BeforeClass
    public static void setupRealm() {
        realmConfig = randomFrom(RealmConfig.values());
        ESLoggerFactory.getLogger("test").info("running test with realm configuration [{}], with direct group to role mapping [{}]",
                realmConfig, realmConfig.mapGroupsAsRoles);
    }

    @AfterClass
    public static void cleanupRealm() {
        realmConfig = null;
    }

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        Path nodeFiles = createTempDir();
        return settingsBuilder()
                .put(super.nodeSettings(nodeOrdinal))
                .put(realmConfig.buildSettings())
                //we need ssl to the LDAP server
                .put(sslSettingsForStore("/org/elasticsearch/shield/transport/ssl/certs/simple/testnode.jks", "testnode"))
                .put(SHIELD_AUTHC_REALMS_EXTERNAL + ".files.role_mapping", writeFile(nodeFiles, "role_mapping.yml", configRoleMappings()))
                .build();
    }

    protected String configRoleMappings() {
        return realmConfig.configRoleMappings();
    }

    @Override
    protected String configRoles() {
        return super.configRoles() +
                "\n" +
                "Avengers:\n" +
                "  cluster: [ NONE ]\n" +
                "  indices:\n" +
                "    - names: 'avengers'\n" +
                "      privileges: [ all ]\n" +
                "SHIELD:\n" +
                "  cluster: [ NONE ]\n" +
                "  indices:\n" +
                "    - names: '" + SHIELD_INDEX + "'\n" +
                "      privileges: [ all ]\n" +
                "Gods:\n" +
                "  cluster: [ NONE ]\n" +
                "  indices:\n" +
                "    - names: '" + ASGARDIAN_INDEX + "'\n" +
                "      privileges: [ all ]\n" +
                "Philanthropists:\n" +
                "  cluster: [ NONE ]\n" +
                "  indices:\n" +
                "    - names: '" + PHILANTHROPISTS_INDEX + "'\n" +
                "      privileges: [ all ]\n";
    }

    protected void assertAccessAllowed(String user, String index) throws IOException {
        Client client = client().filterWithHeader(Collections.singletonMap(BASIC_AUTH_HEADER, userHeader(user, PASSWORD)));
        IndexResponse indexResponse = client.prepareIndex(index, "type").
                setSource(jsonBuilder()
                        .startObject()
                        .field("name", "value")
                        .endObject())
                .execute().actionGet();

        assertThat("user " + user + " should have write access to index " + index, indexResponse.isCreated(), is(true));

        refresh();

        GetResponse getResponse = client.prepareGet(index, "type", indexResponse.getId())
                .get();

        assertThat("user " + user + " should have read access to index " + index, getResponse.getId(), equalTo(indexResponse.getId()));
    }

    protected void assertAccessDenied(String user, String index) throws IOException {
        try {
            client().filterWithHeader(Collections.singletonMap(BASIC_AUTH_HEADER, userHeader(user, PASSWORD)))
                    .prepareIndex(index, "type").
                    setSource(jsonBuilder()
                            .startObject()
                            .field("name", "value")
                            .endObject())
                    .execute().actionGet();
            fail("Write access to index " + index + " should not be allowed for user " + user);
        } catch (ElasticsearchSecurityException e) {
            // expected
        }
        refresh();
    }

    protected static String userHeader(String username, String password) {
        return UsernamePasswordToken.basicAuthHeaderValue(username, new SecuredString(password.toCharArray()));
    }

    private Settings sslSettingsForStore(String resourcePathToStore, String password) {
        Path store = getDataPath(resourcePathToStore);

        if (Files.notExists(store)) {
            throw new ElasticsearchException("store path [" + store + "] doesn't exist");
        }

        return settingsBuilder()
                .put("shield.ssl.keystore.path", store)
                .put("shield.ssl.keystore.password", password)
                .put(ShieldNettyTransport.HOSTNAME_VERIFICATION_SETTING, false)
                .put("shield.ssl.truststore.path", store)
                .put("shield.ssl.truststore.password", password).build();
    }

    /**
     * Represents multiple possible configurations for active directory and ldap
     */
    enum RealmConfig {

        AD(false, AD_ROLE_MAPPING,
                settingsBuilder()
                        .put(SHIELD_AUTHC_REALMS_EXTERNAL + ".type", ActiveDirectoryRealm.TYPE)
                        .put(SHIELD_AUTHC_REALMS_EXTERNAL + ".domain_name", "ad.test.elasticsearch.com")
                        .put(SHIELD_AUTHC_REALMS_EXTERNAL + ".group_search.base_dn", "CN=Users,DC=ad,DC=test,DC=elasticsearch,DC=com")
                        .put(SHIELD_AUTHC_REALMS_EXTERNAL + ".group_search.scope", randomBoolean() ? SUB_TREE : ONE_LEVEL)
                        .put(SHIELD_AUTHC_REALMS_EXTERNAL + ".url", "ldaps://ad.test.elasticsearch.com:636")
                        .build()),

        AD_LDAP_GROUPS_FROM_SEARCH(true, AD_ROLE_MAPPING,
                settingsBuilder()
                        .put(SHIELD_AUTHC_REALMS_EXTERNAL + ".type", LdapRealm.TYPE)
                        .put(SHIELD_AUTHC_REALMS_EXTERNAL + ".url", "ldaps://ad.test.elasticsearch.com:636")
                        .put(SHIELD_AUTHC_REALMS_EXTERNAL + ".group_search.base_dn", "CN=Users,DC=ad,DC=test,DC=elasticsearch,DC=com")
                        .put(SHIELD_AUTHC_REALMS_EXTERNAL + ".group_search.scope", randomBoolean() ? SUB_TREE : ONE_LEVEL)
                        .putArray(SHIELD_AUTHC_REALMS_EXTERNAL + ".user_dn_templates",
                                "cn={0},CN=Users,DC=ad,DC=test,DC=elasticsearch,DC=com")
                        .build()),

        AD_LDAP_GROUPS_FROM_ATTRIBUTE(true, AD_ROLE_MAPPING,
                settingsBuilder()
                        .put(SHIELD_AUTHC_REALMS_EXTERNAL + ".type", LdapRealm.TYPE)
                        .put(SHIELD_AUTHC_REALMS_EXTERNAL + ".url", "ldaps://ad.test.elasticsearch.com:636")
                        .putArray(SHIELD_AUTHC_REALMS_EXTERNAL + ".user_dn_templates",
                                "cn={0},CN=Users,DC=ad,DC=test,DC=elasticsearch,DC=com")
                        .build()),

        OLDAP(false, OLDAP_ROLE_MAPPING,
                settingsBuilder()
                        .put(SHIELD_AUTHC_REALMS_EXTERNAL + ".type", LdapRealm.TYPE)
                        .put(SHIELD_AUTHC_REALMS_EXTERNAL + ".url", "ldaps://54.200.235.244:636")
                        .put(SHIELD_AUTHC_REALMS_EXTERNAL + ".group_search.base_dn",
                                "ou=people, dc=oldap, dc=test, dc=elasticsearch, dc=com")
                        .put(SHIELD_AUTHC_REALMS_EXTERNAL + ".group_search.scope", randomBoolean() ? SUB_TREE : ONE_LEVEL)
                        .putArray(SHIELD_AUTHC_REALMS_EXTERNAL + ".user_dn_templates",
                                "uid={0},ou=people,dc=oldap,dc=test,dc=elasticsearch,dc=com")
                        .build());

        final boolean mapGroupsAsRoles;
        final boolean loginWithCommonName;
        private final String roleMappings;
        private final Settings settings;

        RealmConfig(boolean loginWithCommonName, String roleMappings, Settings settings) {
            this.settings = settings;
            this.loginWithCommonName = loginWithCommonName;
            this.roleMappings = roleMappings;
            this.mapGroupsAsRoles = randomBoolean();
        }

        public Settings buildSettings() {
            return settingsBuilder()
                    .put(SHIELD_AUTHC_REALMS_EXTERNAL + ".order", 1)
                    .put(SHIELD_AUTHC_REALMS_EXTERNAL + ".hostname_verification", false)
                    .put(SHIELD_AUTHC_REALMS_EXTERNAL + ".unmapped_groups_as_roles", mapGroupsAsRoles)
                    .put(this.settings)
                    .build();
        }

        //if mapGroupsAsRoles is turned on we don't write anything to the rolemapping file
        public String configRoleMappings() {
            return mapGroupsAsRoles ? "" : roleMappings;
        }
    }
}
