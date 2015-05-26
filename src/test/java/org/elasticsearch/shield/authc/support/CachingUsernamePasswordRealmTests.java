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

package org.elasticsearch.shield.authc.support;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.shield.User;
import org.elasticsearch.shield.authc.Realm;
import org.elasticsearch.shield.authc.RealmConfig;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;

public class CachingUsernamePasswordRealmTests extends ElasticsearchTestCase {

    private Settings globalSettings;

    @Before
    public void setup() {
        globalSettings = Settings.builder().put("path.home", createTempDir()).build();
    }

    @Test
    public void testSettings() throws Exception {

        String hashAlgo = randomFrom("bcrypt", "bcrypt4", "bcrypt5", "bcrypt6", "bcrypt7", "bcrypt8", "bcrypt9", "sha1", "sha2", "md5", "clear_text", "noop");
        int maxUsers = randomIntBetween(10, 100);
        TimeValue ttl = TimeValue.timeValueMinutes(randomIntBetween(10, 20));
        Settings settings = Settings.builder()
                .put(CachingUsernamePasswordRealm.CACHE_HASH_ALGO_SETTING, hashAlgo)
                .put(CachingUsernamePasswordRealm.CACHE_MAX_USERS_SETTING, maxUsers)
                .put(CachingUsernamePasswordRealm.CACHE_TTL_SETTING, ttl)
                .build();

        RealmConfig config = new RealmConfig("test_realm", settings, globalSettings);
        CachingUsernamePasswordRealm realm = new CachingUsernamePasswordRealm("test", config) {
            @Override
            protected User doAuthenticate(UsernamePasswordToken token) {
                return new User.Simple("username", "r1", "r2", "r3");
            }
        };

        assertThat(realm.hasher, sameInstance(Hasher.resolve(hashAlgo)));
    }

    @Test
    public void testCache(){
        AlwaysAuthenticateCachingRealm realm = new AlwaysAuthenticateCachingRealm(globalSettings);
        SecuredString pass = SecuredStringTests.build("pass");
        realm.authenticate(new UsernamePasswordToken("a", pass));
        realm.authenticate(new UsernamePasswordToken("b", pass));
        realm.authenticate(new UsernamePasswordToken("c", pass));

        assertThat(realm.INVOCATION_COUNTER.intValue(), is(3));
        realm.authenticate(new UsernamePasswordToken("a", pass));
        realm.authenticate(new UsernamePasswordToken("b", pass));
        realm.authenticate(new UsernamePasswordToken("c", pass));

        assertThat(realm.INVOCATION_COUNTER.intValue(), is(3));
    }

    @Test
    public void testCache_changePassword(){
        AlwaysAuthenticateCachingRealm realm = new AlwaysAuthenticateCachingRealm(globalSettings);

        String user = "testUser";
        SecuredString pass1 = SecuredStringTests.build("pass");
        SecuredString pass2 = SecuredStringTests.build("password");

        realm.authenticate(new UsernamePasswordToken(user, pass1));
        realm.authenticate(new UsernamePasswordToken(user, pass1));

        assertThat(realm.INVOCATION_COUNTER.intValue(), is(1));

        realm.authenticate(new UsernamePasswordToken(user, pass2));
        realm.authenticate(new UsernamePasswordToken(user, pass2));

        assertThat(realm.INVOCATION_COUNTER.intValue(), is(2));
    }

    @Test
    public void testAuthenticateContract() throws Exception {
        Realm<UsernamePasswordToken> realm = new FailingAuthenticationRealm(Settings.EMPTY, globalSettings);
        User user = realm.authenticate(new UsernamePasswordToken("user", SecuredStringTests.build("pass")));
        assertThat(user , nullValue());

        realm = new ThrowingAuthenticationRealm(Settings.EMPTY, globalSettings);
        user = realm.authenticate(new UsernamePasswordToken("user", SecuredStringTests.build("pass")));
        assertThat(user , nullValue());
    }

    static class FailingAuthenticationRealm extends CachingUsernamePasswordRealm {

        FailingAuthenticationRealm(Settings settings, Settings global) {
            super("failing", new RealmConfig("failing-test", settings, global));
        }

        @Override
        protected User doAuthenticate(UsernamePasswordToken token) {
            return null;
        }
    }

    static class ThrowingAuthenticationRealm extends CachingUsernamePasswordRealm {

        ThrowingAuthenticationRealm(Settings settings, Settings globalSettings) {
            super("throwing", new RealmConfig("throwing-test", settings, globalSettings));
        }

        @Override
        protected User doAuthenticate(UsernamePasswordToken token) {
            throw new RuntimeException("whatever exception");
        }

    }

    static class AlwaysAuthenticateCachingRealm extends CachingUsernamePasswordRealm {

        public final AtomicInteger INVOCATION_COUNTER = new AtomicInteger(0);

        AlwaysAuthenticateCachingRealm(Settings globalSettings) {
            super("always", new RealmConfig("always-test", Settings.EMPTY, globalSettings));
        }

        @Override
        protected User doAuthenticate(UsernamePasswordToken token) {
            INVOCATION_COUNTER.incrementAndGet();
            return new User.Simple(token.principal(), "testRole1", "testRole2");
        }
    }
}
