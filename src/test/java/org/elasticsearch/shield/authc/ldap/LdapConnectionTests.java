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
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;

public class LdapConnectionTests extends ElasticsearchTestCase {
    public static String SETTINGS_PREFIX = LdapRealm.class.getPackage().getName().substring("com.elasticsearch.".length()) + '.';

    @Rule
    public static ApacheDsRule apacheDsRule = new ApacheDsRule();

    @Test
    public void testBindWithTemplates() {
        String[] ldapUrls = new String[]{apacheDsRule.getUrl()};
        String groupSearchBase = "o=sevenSeas";
        boolean isSubTreeSearch = true;
        String[] userTemplates = new String[]{
                "cn={0},ou=something,ou=obviously,ou=incorrect,o=sevenSeas",
                "wrongname={0},ou=people,o=sevenSeas",
                "cn={0},ou=people,o=sevenSeas", //this last one should work
        };
        StandardLdapConnectionFactory connectionFactory = new StandardLdapConnectionFactory(
                buildLdapSettings(ldapUrls, userTemplates, groupSearchBase, isSubTreeSearch));

        String user = "Horatio Hornblower";
        char[] userPass = "pass".toCharArray();

        LdapConnection ldap = connectionFactory.bind(user, userPass);
        Map<String, String[]> attrs = ldap.getUserAttrs(ldap.getAuthenticatedUserDn());

        assertThat( attrs, hasKey("uid"));
        assertThat( attrs.get("uid"), arrayContaining("hhornblo"));
    }
    @Test
    public void testBindWithBogusTemplates() {
        String[] ldapUrl = new String[]{apacheDsRule.getUrl()};
        String groupSearchBase = "o=sevenSeas";
        boolean isSubTreeSearch = true;
        String[] userTemplates = new String[]{
                "cn={0},ou=something,ou=obviously,ou=incorrect,o=sevenSeas",
                "wrongname={0},ou=people,o=sevenSeas",
                "asdf={0},ou=people,o=sevenSeas", //none of these should work
        };
        StandardLdapConnectionFactory ldapFac = new StandardLdapConnectionFactory(
                buildLdapSettings(ldapUrl, userTemplates, groupSearchBase, isSubTreeSearch));

        String user = "Horatio Hornblower";
        char[] userPass = "pass".toCharArray();

        try {
            LdapConnection ldap = ldapFac.bind(user, userPass);
            fail("bindWithUserTemplates should have failed");
        } catch (LdapException le) {

        }

    }

    @Test
    public void testGroupLookup_Subtree() {
        String groupSearchBase = "o=sevenSeas";
        String userTemplate = "cn={0},ou=people,o=sevenSeas";

        boolean isSubTreeSearch = true;
        StandardLdapConnectionFactory ldapFac = new StandardLdapConnectionFactory(
                buildLdapSettings(apacheDsRule.getUrl(), userTemplate, groupSearchBase, isSubTreeSearch));

        String user = "Horatio Hornblower";
        char[] userPass = "pass".toCharArray();

        LdapConnection ldap = ldapFac.bind(user, userPass);
        List<String> groups = ldap.getGroupsFromSearch(ldap.getAuthenticatedUserDn());
        System.out.println("groups:"+groups);
        assertThat(groups, contains("cn=HMS Lydia,ou=crews,ou=groups,o=sevenSeas"));
    }

    @Test
    public void testGroupLookup_OneLevel() {
        String groupSearchBase = "ou=crews,ou=groups,o=sevenSeas";
        String userTemplate = "cn={0},ou=people,o=sevenSeas";
        boolean isSubTreeSearch = false;
        StandardLdapConnectionFactory ldapFac = new StandardLdapConnectionFactory(
                buildLdapSettings(apacheDsRule.getUrl(), userTemplate, groupSearchBase, isSubTreeSearch));

        String user = "Horatio Hornblower";
        LdapConnection ldap = ldapFac.bind(user, "pass".toCharArray());

        List<String> groups = ldap.getGroupsFromSearch(ldap.getAuthenticatedUserDn());
        System.out.println("groups:"+groups);
        assertThat(groups, contains("cn=HMS Lydia,ou=crews,ou=groups,o=sevenSeas"));
    }

    public static Settings buildLdapSettings(String ldapUrl, String userTemplate, String groupSearchBase, boolean isSubTreeSearch) {
        return buildLdapSettings( new String[]{ldapUrl}, new String[]{userTemplate}, groupSearchBase, isSubTreeSearch );
    }

    public static Settings buildLdapSettings(String[] ldapUrl, String[] userTemplate, String groupSearchBase, boolean isSubTreeSearch) {
        return ImmutableSettings.builder()
                .putArray(SETTINGS_PREFIX + StandardLdapConnectionFactory.URLS_SETTING, ldapUrl)
                .putArray(SETTINGS_PREFIX + StandardLdapConnectionFactory.USER_DN_TEMPLATES_SETTING, userTemplate)
                .put(SETTINGS_PREFIX + StandardLdapConnectionFactory.GROUP_SEARCH_BASEDN_SETTING, groupSearchBase)
                .put(SETTINGS_PREFIX + StandardLdapConnectionFactory.GROUP_SEARCH_SUBTREE_SETTING, isSubTreeSearch).build();
    }

}
