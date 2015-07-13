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

package org.elasticsearch.shield.authc.ldap.support;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPURL;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.authc.RealmConfig;
import org.elasticsearch.shield.authc.ldap.LdapRealm;
import org.elasticsearch.shield.authc.support.DnRoleMapper;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.elasticsearch.watcher.ResourceWatcherService;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;

import static org.elasticsearch.shield.authc.ldap.LdapSessionFactory.*;

@Ignore
public abstract class LdapTest extends ElasticsearchTestCase {

    protected InMemoryDirectoryServer ldapServer;

    @Before
    public void startLdap() throws Exception {
        ldapServer = new InMemoryDirectoryServer("o=sevenSeas");
        ldapServer.add("o=sevenSeas", new Attribute("dc", "UnboundID"), new Attribute("objectClass", "top", "domain", "extensibleObject"));
        ldapServer.importFromLDIF(false, getDataPath("/org/elasticsearch/shield/authc/ldap/support/seven-seas.ldif").toString());
        ldapServer.startListening();
    }

    @After
    public void stopLdap() throws Exception {
        ldapServer.shutDown(true);
    }

    protected String ldapUrl() throws LDAPException {
        LDAPURL url = new LDAPURL("ldap", "localhost", ldapServer.getListenPort(), null, null, null, null);
        return url.toString();
    }

    public static Settings buildLdapSettings(String ldapUrl, String userTemplate, String groupSearchBase, LdapSearchScope scope) {
        return buildLdapSettings(ldapUrl, new String[] { userTemplate }, groupSearchBase, scope);
    }

    public static Settings buildLdapSettings(String ldapUrl, String[] userTemplate, String groupSearchBase, LdapSearchScope scope) {
        return Settings.builder()
                .putArray(URLS_SETTING, ldapUrl)
                .putArray(USER_DN_TEMPLATES_SETTING, userTemplate)
                .put("group_search.base_dn", groupSearchBase)
                .put("group_search.scope", scope)
                .put(HOSTNAME_VERIFICATION_SETTING, false)
                .build();
    }

    public static Settings buildLdapSettings(String ldapUrl, String userTemplate, boolean hostnameVerification) {
        return Settings.builder()
                .putArray(URLS_SETTING, ldapUrl)
                .putArray(USER_DN_TEMPLATES_SETTING, userTemplate)
                .put(HOSTNAME_VERIFICATION_SETTING, hostnameVerification)
                .build();
    }

    protected DnRoleMapper buildGroupAsRoleMapper(ResourceWatcherService resourceWatcherService) {
        Settings settings = Settings.builder()
                .put(DnRoleMapper.USE_UNMAPPED_GROUPS_AS_ROLES_SETTING, true)
                .build();
        Settings global = Settings.builder().put("path.home", createTempDir()).build();
        RealmConfig config = new RealmConfig("ldap1", settings, global);

        return new DnRoleMapper(LdapRealm.TYPE, config, resourceWatcherService, null);
    }
}
