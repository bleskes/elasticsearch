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
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;

@Ignore
public abstract class LdapTest extends ElasticsearchTestCase {

    static String SETTINGS_PREFIX = LdapRealm.class.getPackage().getName().substring("com.elasticsearch.".length()) + '.';

    private static final TemporaryFolder temporaryFolder = new TemporaryFolder();

    protected static final ApacheDsRule apacheDsRule = new ApacheDsRule(temporaryFolder);

    @ClassRule
    public static final RuleChain ruleChain = RuleChain.outerRule(temporaryFolder).around(apacheDsRule);

    static Settings buildLdapSettings(String ldapUrl, String userTemplate, String groupSearchBase, boolean isSubTreeSearch) {
        return buildLdapSettings( new String[]{ldapUrl}, new String[]{userTemplate}, groupSearchBase, isSubTreeSearch );
    }

    static Settings buildLdapSettings(String[] ldapUrl, String[] userTemplate, String groupSearchBase, boolean isSubTreeSearch) {
        return ImmutableSettings.builder()
                .putArray(SETTINGS_PREFIX + StandardLdapConnectionFactory.URLS_SETTING, ldapUrl)
                .putArray(SETTINGS_PREFIX + StandardLdapConnectionFactory.USER_DN_TEMPLATES_SETTING, userTemplate)
                .put(SETTINGS_PREFIX + StandardLdapConnectionFactory.GROUP_SEARCH_BASEDN_SETTING, groupSearchBase)
                .put(SETTINGS_PREFIX + StandardLdapConnectionFactory.GROUP_SEARCH_SUBTREE_SETTING, isSubTreeSearch).build();
    }
}
