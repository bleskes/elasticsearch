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
import org.elasticsearch.env.Environment;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.shield.User;
import org.elasticsearch.shield.authc.support.UsernamePasswordToken;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.watcher.ResourceWatcherService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class LdapRealmTest extends ElasticsearchTestCase {

    public static String AD_IP = "54.213.145.20";
    public static String AD_URL = "ldap://" + AD_IP + ":389";

    public static final String VALID_USER_TEMPLATE = "cn={0},ou=people,o=sevenSeas";
    public static final String VALID_USERNAME = "Thomas Masterman Hardy";
    public static final String PASSWORD = "pass";

    private RestController restController;

    @Before
    public void init() throws Exception {
        restController = mock(RestController.class);
    }

    @Rule
    public static ApacheDsRule apacheDsRule = new ApacheDsRule();

    @Test
    public void testRestHeaderRegistration() {
        new LdapRealm(ImmutableSettings.EMPTY, mock(LdapConnectionFactory.class), mock(LdapGroupToRoleMapper.class), restController);
        verify(restController).registerRelevantHeaders(UsernamePasswordToken.BASIC_AUTH_HEADER);
    }

    @Test
    public void testAuthenticate_subTreeGroupSearch(){
        String groupSearchBase = "o=sevenSeas";
        boolean isSubTreeSearch = true;
        String userTemplate = VALID_USER_TEMPLATE;
        Settings settings = LdapConnectionTests.buildLdapSettings(apacheDsRule.getUrl(), userTemplate, groupSearchBase, isSubTreeSearch);
        StandardLdapConnectionFactory ldapFactory = new StandardLdapConnectionFactory(settings);
        LdapRealm ldap = new LdapRealm(buildNonCachingSettings(), ldapFactory, buildGroupAsRoleMapper(), restController);

        User user = ldap.authenticate(new UsernamePasswordToken(VALID_USERNAME, PASSWORD.toCharArray()));
        assertThat( user, notNullValue());
        assertThat(user.roles(), arrayContaining("HMS Victory"));
    }

    @Test
    public void testAuthenticate_oneLevelGroupSearch(){
        String groupSearchBase = "ou=crews,ou=groups,o=sevenSeas";
        boolean isSubTreeSearch = false;
        String userTemplate = VALID_USER_TEMPLATE;
        StandardLdapConnectionFactory ldapFactory = new StandardLdapConnectionFactory(
                LdapConnectionTests.buildLdapSettings(apacheDsRule.getUrl(), userTemplate, groupSearchBase, isSubTreeSearch));

        LdapRealm ldap = new LdapRealm(buildNonCachingSettings(), ldapFactory, buildGroupAsRoleMapper(), restController);

        User user = ldap.authenticate(new UsernamePasswordToken(VALID_USERNAME, PASSWORD.toCharArray()));
        assertThat( user, notNullValue());
        assertThat( user.roles(), arrayContaining("HMS Victory"));
    }

    @Ignore //this is still failing.  not sure why.
    @Test
    public void testAuthenticate_caching(){
        String groupSearchBase = "o=sevenSeas";
        boolean isSubTreeSearch = true;
        String userTemplate = VALID_USER_TEMPLATE;
        StandardLdapConnectionFactory ldapFactory = new StandardLdapConnectionFactory(
                LdapConnectionTests.buildLdapSettings( apacheDsRule.getUrl(), userTemplate, groupSearchBase, isSubTreeSearch) );

        ldapFactory = spy(ldapFactory);
        LdapRealm ldap = new LdapRealm( buildCachingSettings(), ldapFactory, buildGroupAsRoleMapper(), restController);
        User user = ldap.authenticate( new UsernamePasswordToken(VALID_USERNAME, PASSWORD.toCharArray()));
        user = ldap.authenticate( new UsernamePasswordToken(VALID_USERNAME, PASSWORD.toCharArray()));

        //verify one and only one bind -> caching is working
        verify(ldapFactory, times(1)).bind(anyString(), any(char[].class));
    }

    @Test
    public void testAuthenticate_noncaching(){
        String groupSearchBase = "o=sevenSeas";
        boolean isSubTreeSearch = true;
        String userTemplate = VALID_USER_TEMPLATE;
        StandardLdapConnectionFactory ldapFactory = new StandardLdapConnectionFactory(
                LdapConnectionTests.buildLdapSettings(apacheDsRule.getUrl(), userTemplate, groupSearchBase, isSubTreeSearch) );

        ldapFactory = spy(ldapFactory);
        LdapRealm ldap = new LdapRealm( buildNonCachingSettings(), ldapFactory, buildGroupAsRoleMapper(), restController);
        User user = ldap.authenticate( new UsernamePasswordToken(VALID_USERNAME, PASSWORD.toCharArray()));
        user = ldap.authenticate( new UsernamePasswordToken(VALID_USERNAME, PASSWORD.toCharArray()));

        //verify two and only two binds -> caching is disabled
        verify(ldapFactory, times(2)).bind(anyString(), any(char[].class));
    }

    @Ignore
    @Test
    public void testAD() {
        String adDomain = "ad.test.elasticsearch.com";
        String userSearchBaseDN = "dc=ad,dc=es,dc=com";

        ActiveDirectoryConnectionFactory ldapFactory = new ActiveDirectoryConnectionFactory(
                ActiveDirectoryFactoryTests.buildAdSettings(AD_URL, adDomain));

        LdapRealm ldap = new LdapRealm( buildNonCachingSettings(), ldapFactory, buildGroupAsRoleMapper(), restController);

        User user = ldap.authenticate( new UsernamePasswordToken("george", "R))Tr0x".toCharArray()));

        assertThat( user, notNullValue());
        assertThat( user.roles(), hasItemInArray("upchuckers"));
    }

    @Ignore
    @Test
    public void testAD_defaults() {
        //only set the adDomain, and see if it infers the rest correctly
        String adDomain = AD_IP;
        Settings settings = ImmutableSettings.builder()
                .put(LdapConnectionTests.SETTINGS_PREFIX + ActiveDirectoryConnectionFactory.AD_DOMAIN_NAME_SETTING, adDomain)
                .build();

        ActiveDirectoryConnectionFactory ldapFactory = new ActiveDirectoryConnectionFactory( settings );
        LdapRealm ldap = new LdapRealm( buildNonCachingSettings(), ldapFactory, buildGroupAsRoleMapper(), restController);
        User user = ldap.authenticate( new UsernamePasswordToken("george", "R))Tr0x".toCharArray()));

        assertThat( user, notNullValue());
        assertThat( user.roles(), hasItemInArray("upchuckers"));
    }



    private Settings buildNonCachingSettings() {
        return ImmutableSettings.builder()
                .put("shield.authc.ldap."+LdapRealm.CACHE_TTL, -1)
                .build();
    }

    private Settings buildCachingSettings() {
        return ImmutableSettings.builder()
                .put("shield.authc.ldap."+LdapRealm.CACHE_TTL, 1)
                .put("shield.authc.ldap."+LdapRealm.CACHE_MAX_USERS, 10)
                .build();
    }

    private LdapGroupToRoleMapper buildGroupAsRoleMapper() {
        Settings settings = ImmutableSettings.builder()
                .put("shield.authc.ldap." + LdapGroupToRoleMapper.USE_UNMAPPED_GROUPS_AS_ROLES_SETTING, true)
                .build();

        return new LdapGroupToRoleMapper(settings,
                new Environment(settings),
                new ResourceWatcherService(settings, new ThreadPool("test")));

    }
}
