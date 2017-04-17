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

package org.elasticsearch.xpack.security.authc.ldap.support;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPURL;
import org.elasticsearch.action.support.PlainActionFuture;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.SecureString;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.xpack.security.authc.RealmConfig;
import org.elasticsearch.xpack.security.authc.ldap.LdapRealm;
import org.elasticsearch.xpack.security.authc.ldap.LdapSessionFactory;
import org.elasticsearch.xpack.security.authc.support.DnRoleMapper;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.watcher.ResourceWatcherService;
import org.elasticsearch.xpack.ssl.VerificationMode;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.elasticsearch.xpack.security.authc.ldap.LdapSessionFactory.HOSTNAME_VERIFICATION_SETTING;
import static org.elasticsearch.xpack.security.authc.ldap.LdapSessionFactory.URLS_SETTING;

public abstract class LdapTestCase extends ESTestCase {

    private static final String USER_DN_TEMPLATES_SETTING_KEY = LdapSessionFactory.USER_DN_TEMPLATES_SETTING.getKey();

    static int numberOfLdapServers;
    protected InMemoryDirectoryServer[] ldapServers;

    @BeforeClass
    public static void setNumberOfLdapServers() {
        numberOfLdapServers = randomIntBetween(1, 4);
    }

    @Before
    public void startLdap() throws Exception {
        ldapServers = new InMemoryDirectoryServer[numberOfLdapServers];
        for (int i = 0; i < numberOfLdapServers; i++) {
            InMemoryDirectoryServer ldapServer = new InMemoryDirectoryServer("o=sevenSeas");
            ldapServer.add("o=sevenSeas", new Attribute("dc", "UnboundID"),
                    new Attribute("objectClass", "top", "domain", "extensibleObject"));
            ldapServer.importFromLDIF(false,
                    getDataPath("/org/elasticsearch/xpack/security/authc/ldap/support/seven-seas.ldif").toString());
            ldapServer.startListening();
            ldapServers[i] = ldapServer;
        }
    }

    @After
    public void stopLdap() throws Exception {
        for (int i = 0; i < numberOfLdapServers; i++) {
            ldapServers[i].shutDown(true);
        }
    }

    protected String[] ldapUrls() throws LDAPException {
        List<String> urls = new ArrayList<>(numberOfLdapServers);
        for (int i = 0; i < numberOfLdapServers; i++) {
            LDAPURL url = new LDAPURL("ldap", "localhost", ldapServers[i].getListenPort(), null, null, null, null);
            urls.add(url.toString());
        }
        return urls.toArray(Strings.EMPTY_ARRAY);
    }

    public static Settings buildLdapSettings(String ldapUrl, String userTemplate, String groupSearchBase, LdapSearchScope scope) {
        return buildLdapSettings(new String[] { ldapUrl }, new String[] { userTemplate }, groupSearchBase, scope);
    }

    public static Settings buildLdapSettings(String[] ldapUrl, String userTemplate, String groupSearchBase, LdapSearchScope scope) {
        return buildLdapSettings(ldapUrl, new String[] { userTemplate }, groupSearchBase, scope);
    }

    public static Settings buildLdapSettings(String[] ldapUrl, String[] userTemplate, String groupSearchBase, LdapSearchScope scope) {
        return buildLdapSettings(ldapUrl, userTemplate, groupSearchBase, scope, null);
    }

    public static Settings buildLdapSettings(String[] ldapUrl, String[] userTemplate,
                                             String groupSearchBase, LdapSearchScope scope,
                                             LdapLoadBalancing serverSetType) {
        return buildLdapSettings(ldapUrl, userTemplate, groupSearchBase, scope,
                serverSetType, false);
    }

    public static Settings buildLdapSettings(String[] ldapUrl, String[] userTemplate,
                                             String groupSearchBase, LdapSearchScope scope,
                                             LdapLoadBalancing serverSetType,
                                             boolean ignoreReferralErrors) {
        Settings.Builder builder = Settings.builder()
                .putArray(URLS_SETTING, ldapUrl)
                .putArray(USER_DN_TEMPLATES_SETTING_KEY, userTemplate)
                .put(SessionFactory.IGNORE_REFERRAL_ERRORS_SETTING.getKey(), ignoreReferralErrors)
                .put("group_search.base_dn", groupSearchBase)
                .put("group_search.scope", scope)
                .put("ssl.verification_mode", VerificationMode.CERTIFICATE);
        if (serverSetType != null) {
            builder.put(LdapLoadBalancing.LOAD_BALANCE_SETTINGS + "." +
                            LdapLoadBalancing.LOAD_BALANCE_TYPE_SETTING, serverSetType.toString());
        }
        return builder.build();
    }

    public static Settings buildLdapSettings(String[] ldapUrl, String userTemplate, boolean hostnameVerification) {
        Settings.Builder builder = Settings.builder()
                .putArray(URLS_SETTING, ldapUrl)
                .putArray(USER_DN_TEMPLATES_SETTING_KEY, userTemplate);
        if (randomBoolean()) {
            builder.put("ssl.verification_mode", hostnameVerification ? VerificationMode.FULL : VerificationMode.CERTIFICATE);
        } else {
            builder.put(HOSTNAME_VERIFICATION_SETTING, hostnameVerification);
        }
        return builder.build();
    }

    protected DnRoleMapper buildGroupAsRoleMapper(ResourceWatcherService resourceWatcherService) {
        Settings settings = Settings.builder()
                .put(DnRoleMapper.USE_UNMAPPED_GROUPS_AS_ROLES_SETTING.getKey(), true)
                .build();
        Settings global = Settings.builder().put("path.home", createTempDir()).build();
        RealmConfig config = new RealmConfig("ldap1", settings, global, new ThreadContext(Settings.EMPTY));

        return new DnRoleMapper(LdapRealm.LDAP_TYPE, config, resourceWatcherService);
    }

    protected LdapSession session(SessionFactory factory, String username, SecureString password) {
        PlainActionFuture<LdapSession> future = new PlainActionFuture<>();
        factory.session(username, password, future);
        return future.actionGet();
    }

    protected List<String> groups(LdapSession ldapSession) {
        Objects.requireNonNull(ldapSession);
        PlainActionFuture<List<String>> future = new PlainActionFuture<>();
        ldapSession.groups(future);
        return future.actionGet();
    }

    protected LdapSession unauthenticatedSession(SessionFactory factory, String username) {
        PlainActionFuture<LdapSession> future = new PlainActionFuture<>();
        factory.unauthenticatedSession(username, future);
        return future.actionGet();
    }
}
