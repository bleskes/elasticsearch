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
import org.elasticsearch.rest.RestController;
import org.elasticsearch.shield.User;
import org.elasticsearch.shield.authc.RealmConfig;
import org.elasticsearch.shield.authc.support.SecuredString;
import org.elasticsearch.shield.authc.support.SecuredStringTests;
import org.elasticsearch.shield.authc.support.UsernamePasswordToken;
import org.elasticsearch.shield.authc.support.ldap.LdapTest;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.watcher.ResourceWatcherService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class LdapRealmTest extends LdapTest {

    public static final String VALID_USER_TEMPLATE = "cn={0},ou=people,o=sevenSeas";
    public static final String VALID_USERNAME = "Thomas Masterman Hardy";
    public static final String PASSWORD = "pass";

    private RestController restController;
    private ThreadPool threadPool;
    private ResourceWatcherService resourceWatcherService;

    @Before
    public void init() throws Exception {
        restController = mock(RestController.class);
        threadPool = new ThreadPool("test");
        resourceWatcherService = new ResourceWatcherService(ImmutableSettings.EMPTY, threadPool);
    }

    @After
    public void shutdown() {
        resourceWatcherService.stop();
        threadPool.shutdownNow();
    }

    @Test
    public void testRestHeaderRegistration() {
        new LdapRealm.Factory(resourceWatcherService, restController);
        verify(restController).registerRelevantHeaders(UsernamePasswordToken.BASIC_AUTH_HEADER);
    }

    @Test
    public void testAuthenticate_SubTreeGroupSearch(){
        String groupSearchBase = "o=sevenSeas";
        boolean isSubTreeSearch = true;
        String userTemplate = VALID_USER_TEMPLATE;
        Settings settings = buildLdapSettings(ldapUrl(), userTemplate, groupSearchBase, isSubTreeSearch);
        RealmConfig config = new RealmConfig("test-ldap-realm", settings);
        LdapConnectionFactory ldapFactory = new LdapConnectionFactory(config);
        config = new RealmConfig("test-ldap-realm", buildNonCachingSettings());
        LdapRealm ldap = new LdapRealm(config, ldapFactory, buildGroupAsRoleMapper(resourceWatcherService));

        User user = ldap.authenticate(new UsernamePasswordToken(VALID_USERNAME, SecuredStringTests.build(PASSWORD)));
        assertThat( user, notNullValue());
        assertThat(user.roles(), arrayContaining("HMS Victory"));
    }

    @Test
    public void testAuthenticate_OneLevelGroupSearch(){
        String groupSearchBase = "ou=crews,ou=groups,o=sevenSeas";
        boolean isSubTreeSearch = false;
        String userTemplate = VALID_USER_TEMPLATE;
        Settings settings = ImmutableSettings.builder()
                .put(buildLdapSettings(ldapUrl(), userTemplate, groupSearchBase, isSubTreeSearch))
                .put(buildNonCachingSettings())
                .build();
        RealmConfig config = new RealmConfig("test-ldap-realm", settings);

        LdapConnectionFactory ldapFactory = new LdapConnectionFactory(config);
        LdapRealm ldap = new LdapRealm(config, ldapFactory, buildGroupAsRoleMapper(resourceWatcherService));

        User user = ldap.authenticate(new UsernamePasswordToken(VALID_USERNAME, SecuredStringTests.build(PASSWORD)));
        assertThat( user, notNullValue());
        assertThat(user.roles(), arrayContaining("HMS Victory"));
    }

    @Test
    public void testAuthenticate_Caching(){
        String groupSearchBase = "o=sevenSeas";
        boolean isSubTreeSearch = true;
        String userTemplate = VALID_USER_TEMPLATE;
        Settings settings = ImmutableSettings.builder()
                .put(buildLdapSettings(ldapUrl(), userTemplate, groupSearchBase, isSubTreeSearch))
                .put(buildCachingSettings())
                .build();
        RealmConfig config = new RealmConfig("test-ldap-realm", settings);

        LdapConnectionFactory ldapFactory = new LdapConnectionFactory(config);
        ldapFactory = spy(ldapFactory);
        LdapRealm ldap = new LdapRealm(config, ldapFactory, buildGroupAsRoleMapper(resourceWatcherService));
        User user = ldap.authenticate( new UsernamePasswordToken(VALID_USERNAME, SecuredStringTests.build(PASSWORD)));
        user = ldap.authenticate( new UsernamePasswordToken(VALID_USERNAME, SecuredStringTests.build(PASSWORD)));

        //verify one and only one open -> caching is working
        verify(ldapFactory, times(1)).open(anyString(), any(SecuredString.class));
    }

    @Test
    public void testAuthenticate_Caching_Refresh(){
        String groupSearchBase = "o=sevenSeas";
        boolean isSubTreeSearch = true;
        String userTemplate = VALID_USER_TEMPLATE;
        Settings settings = ImmutableSettings.builder()
                .put(buildLdapSettings(ldapUrl(), userTemplate, groupSearchBase, isSubTreeSearch))
                .put(buildCachingSettings())
                .build();
        RealmConfig config = new RealmConfig("test-ldap-realm", settings);

        LdapConnectionFactory ldapFactory = new LdapConnectionFactory(config);
        LdapGroupToRoleMapper roleMapper = buildGroupAsRoleMapper(resourceWatcherService);
        ldapFactory = spy(ldapFactory);
        LdapRealm ldap = new LdapRealm(config, ldapFactory, roleMapper);
        User user = ldap.authenticate( new UsernamePasswordToken(VALID_USERNAME, SecuredStringTests.build(PASSWORD)));
        user = ldap.authenticate( new UsernamePasswordToken(VALID_USERNAME, SecuredStringTests.build(PASSWORD)));

        //verify one and only one open -> caching is working
        verify(ldapFactory, times(1)).open(anyString(), any(SecuredString.class));

        roleMapper.notifyRefresh();

        user = ldap.authenticate( new UsernamePasswordToken(VALID_USERNAME, SecuredStringTests.build(PASSWORD)));

        //we need to open again
        verify(ldapFactory, times(2)).open(anyString(), any(SecuredString.class));
    }

    @Test
    public void testAuthenticate_Noncaching(){
        String groupSearchBase = "o=sevenSeas";
        boolean isSubTreeSearch = true;
        String userTemplate = VALID_USER_TEMPLATE;
        Settings settings = ImmutableSettings.builder()
                .put(buildLdapSettings(ldapUrl(), userTemplate, groupSearchBase, isSubTreeSearch))
                .put(buildNonCachingSettings())
                .build();
        RealmConfig config = new RealmConfig("test-ldap-realm", settings);

        LdapConnectionFactory ldapFactory = new LdapConnectionFactory(config);
        ldapFactory = spy(ldapFactory);
        LdapRealm ldap = new LdapRealm(config, ldapFactory, buildGroupAsRoleMapper(resourceWatcherService));
        User user = ldap.authenticate( new UsernamePasswordToken(VALID_USERNAME, SecuredStringTests.build(PASSWORD)));
        user = ldap.authenticate( new UsernamePasswordToken(VALID_USERNAME, SecuredStringTests.build(PASSWORD)));

        //verify two and only two binds -> caching is disabled
        verify(ldapFactory, times(2)).open(anyString(), any(SecuredString.class));
    }


}
