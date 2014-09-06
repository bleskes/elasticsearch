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

import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.shield.User;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class CachingUsernamePasswordRealmTests {
    public static class AlwaysAuthenticateCachingRealm extends CachingUsernamePasswordRealm {
        public AlwaysAuthenticateCachingRealm() {
            super(ImmutableSettings.EMPTY);
        }
        public final AtomicInteger INVOCATION_COUNTER = new AtomicInteger(0);
        @Override protected User doAuthenticate(UsernamePasswordToken token) {
            INVOCATION_COUNTER.incrementAndGet();
            return new User.Simple(token.principal(), "testRole1", "testRole2");
        }

        @Override public String type() { return "test"; }
    }


    @Test
    public void testCache(){
        AlwaysAuthenticateCachingRealm realm = new AlwaysAuthenticateCachingRealm();
        char[] pass = "pass".toCharArray();
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
        AlwaysAuthenticateCachingRealm realm = new AlwaysAuthenticateCachingRealm();

        String user = "testUser";
        char[] pass1 = "pass".toCharArray();
        char[] pass2 = "password".toCharArray();

        realm.authenticate(new UsernamePasswordToken(user, pass1));
        realm.authenticate(new UsernamePasswordToken(user, pass1));

        assertThat(realm.INVOCATION_COUNTER.intValue(), is(1));

        realm.authenticate(new UsernamePasswordToken(user, pass2));
        realm.authenticate(new UsernamePasswordToken(user, pass2));

        assertThat(realm.INVOCATION_COUNTER.intValue(), is(2));
    }
}
