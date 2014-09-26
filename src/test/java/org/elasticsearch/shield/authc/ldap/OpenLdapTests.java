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

import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.shield.authc.support.SecuredStringTests;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.elasticsearch.test.junit.annotations.Network;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;

@Network
public class OpenLdapTests extends ElasticsearchTestCase {
    public static final String OPEN_LDAP_URL = "ldaps://54.200.235.244:636";
    public static final String PASSWORD = "NickFuryHeartsES";
    public static final String SETTINGS_PREFIX = LdapRealm.class.getPackage().getName().substring("com.elasticsearch.".length()) + '.';

    @BeforeClass
    public static void setTrustStore() throws URISyntaxException {
        //LdapModule will set this up as a singleton normally
        LdapSslSocketFactory.init(ImmutableSettings.builder()
                .put(SETTINGS_PREFIX + "truststore", new File(LdapConnectionTests.class.getResource("ldaptrust.jks").toURI()))
                .build());
    }

    @Test
    public void test_standardLdapConnection_uid(){
        //openldap does not use cn as naming attributes by default

        String groupSearchBase = "ou=people,dc=oldap,dc=test,dc=elasticsearch,dc=com";
        String userTemplate = "uid={0},ou=people,dc=oldap,dc=test,dc=elasticsearch,dc=com";
        boolean isSubTreeSearch = true;
        StandardLdapConnectionFactory connectionFactory = new StandardLdapConnectionFactory(
                LdapConnectionTests.buildLdapSettings(OPEN_LDAP_URL, userTemplate, groupSearchBase, isSubTreeSearch));

        String[] users = new String[]{"blackwidow", "cap", "hawkeye", "hulk", "ironman", "thor"};
        for(String user: users) {
            LdapConnection ldap = connectionFactory.bind(user, SecuredStringTests.build(PASSWORD));
            assertThat(ldap.getGroups(), hasItem(containsString("Avengers")));
            ldap.close();
        }
    }

}
