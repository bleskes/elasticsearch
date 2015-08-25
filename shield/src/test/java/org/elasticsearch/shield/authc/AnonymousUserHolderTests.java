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

package org.elasticsearch.shield.authc;

import org.elasticsearch.common.io.stream.ByteBufferStreamInput;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.User;
import org.elasticsearch.test.ESTestCase;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.hamcrest.Matchers.*;

public class AnonymousUserHolderTests extends ESTestCase {

    @Test
    public void testResolveAnonymousUser() throws Exception {
        Settings settings = Settings.builder()
                .put("shield.authc.anonymous.username", "anonym1")
                .putArray("shield.authc.anonymous.roles", "r1", "r2", "r3")
                .build();
        User user = AnonymousService.resolveAnonymousUser(settings);
        assertThat(user, notNullValue());
        assertThat(user.principal(), equalTo("anonym1"));
        assertThat(user.roles(), arrayContainingInAnyOrder("r1", "r2", "r3"));

        settings = Settings.builder()
                .putArray("shield.authc.anonymous.roles", "r1", "r2", "r3")
                .build();
        user = AnonymousService.resolveAnonymousUser(settings);
        assertThat(user, notNullValue());
        assertThat(user.principal(), equalTo(AnonymousService.ANONYMOUS_USERNAME));
        assertThat(user.roles(), arrayContainingInAnyOrder("r1", "r2", "r3"));
    }

    @Test
    public void testResolveAnonymousUser_NoSettings() throws Exception {
        Settings settings = randomBoolean() ?
                Settings.EMPTY :
                Settings.builder().put("shield.authc.anonymous.username", "user1").build();
        User user = AnonymousService.resolveAnonymousUser(settings);
        assertThat(user, nullValue());
    }

    @Test
    public void testWhenAnonymousDisabled() {
        AnonymousService anonymousService = new AnonymousService(Settings.EMPTY);
        assertThat(anonymousService.enabled(), is(false));
        assertThat(anonymousService.isAnonymous(new User.Simple(randomAsciiOfLength(10), new String[] { randomAsciiOfLength(5) })), is(false));
        assertThat(anonymousService.anonymousUser(), nullValue());
        assertThat(anonymousService.authorizationExceptionsEnabled(), is(true));
    }

    @Test
    public void testWhenAnonymousEnabled() throws Exception {
        Settings settings = Settings.builder()
                .putArray("shield.authc.anonymous.roles", "r1", "r2", "r3")
                .build();
        AnonymousService anonymousService = new AnonymousService(settings);
        assertThat(anonymousService.enabled(), is(true));
        assertThat(anonymousService.anonymousUser(), notNullValue());
        assertThat(anonymousService.isAnonymous(anonymousService.anonymousUser()), is(true));
        assertThat(anonymousService.authorizationExceptionsEnabled(), is(true));

        // make sure check works with serialization
        BytesStreamOutput output = new BytesStreamOutput();
        User.writeTo(anonymousService.anonymousUser(), output);
        User anonymousSerialized = User.readFrom(new ByteBufferStreamInput(ByteBuffer.wrap(output.bytes().toBytes())));
        assertThat(anonymousService.isAnonymous(anonymousSerialized), is(true));
    }

    @Test
    public void testDisablingAuthorizationExceptions() {
        Settings settings = Settings.builder()
                .putArray("shield.authc.anonymous.roles", "r1", "r2", "r3")
                .put(AnonymousService.SETTING_AUTHORIZATION_EXCEPTION_ENABLED, false)
                .build();
        AnonymousService holder = new AnonymousService(settings);
        assertThat(holder.authorizationExceptionsEnabled(), is(false));
    }
}
