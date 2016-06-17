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

package org.elasticsearch.shield.authz;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.shield.authc.Authentication;
import org.elasticsearch.shield.authc.Authentication.RealmRef;
import org.elasticsearch.shield.user.SystemUser;
import org.elasticsearch.shield.user.User;
import org.elasticsearch.test.ESTestCase;
import org.junit.Before;

import static org.hamcrest.Matchers.is;

/**
 * Unit tests for the AuthorizationUtils class
 */
public class AuthorizationUtilsTests extends ESTestCase {

    private ThreadContext threadContext;

    @Before
    public void setupContext() {
        threadContext = new ThreadContext(Settings.EMPTY);
    }

    public void testSystemUserSwitchNonInternalAction() {
        assertThat(AuthorizationUtils.shouldReplaceUserWithSystem(threadContext, randomFrom("indices:foo", "cluster:bar")), is(false));
    }

    public void testSystemUserSwitchWithNullorSystemUser() {
        if (randomBoolean()) {
            threadContext.putTransient(Authentication.AUTHENTICATION_KEY,
                    new Authentication(SystemUser.INSTANCE, new RealmRef("test", "test", "foo"), null));
        }
        assertThat(AuthorizationUtils.shouldReplaceUserWithSystem(threadContext, "internal:something"), is(true));
    }

    public void testSystemUserSwitchWithNonSystemUser() {
        User user = new User(randomAsciiOfLength(6), new String[] {});
        Authentication authentication =  new Authentication(user, new RealmRef("test", "test", "foo"), null);
        threadContext.putTransient(Authentication.AUTHENTICATION_KEY, authentication);
        threadContext.putTransient(InternalAuthorizationService.ORIGINATING_ACTION_KEY, randomFrom("indices:foo", "cluster:bar"));
        assertThat(AuthorizationUtils.shouldReplaceUserWithSystem(threadContext, "internal:something"), is(true));
    }

    public void testSystemUserSwitchWithNonSystemUserAndInternalAction() {
        User user = new User(randomAsciiOfLength(6), new String[] {});
        Authentication authentication =  new Authentication(user, new RealmRef("test", "test", "foo"), null);
        threadContext.putTransient(Authentication.AUTHENTICATION_KEY, authentication);
        threadContext.putTransient(InternalAuthorizationService.ORIGINATING_ACTION_KEY, randomFrom("internal:foo/bar"));
        assertThat(AuthorizationUtils.shouldReplaceUserWithSystem(threadContext, "internal:something"), is(false));
    }
}
