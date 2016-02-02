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

package org.elasticsearch.shield;

import org.elasticsearch.ElasticsearchSecurityException;
import org.elasticsearch.common.io.stream.ByteBufferStreamInput;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.test.ESTestCase;

import java.util.Arrays;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;

public class UserTests extends ESTestCase {

    public void testWriteToAndReadFrom() throws Exception {
        User user = new User(randomAsciiOfLengthBetween(4, 30),
                generateRandomStringArray(20, 30, false));
        BytesStreamOutput output = new BytesStreamOutput();

        User.writeTo(user, output);
        User readFrom = User.readFrom(ByteBufferStreamInput.wrap(output.bytes()));

        assertThat(readFrom, not(sameInstance(user)));
        assertThat(readFrom.principal(), is(user.principal()));
        assertThat(Arrays.equals(readFrom.roles(), user.roles()), is(true));
        assertThat(readFrom.runAs(), is(nullValue()));
    }

    public void testWriteToAndReadFromWithRunAs() throws Exception {
        User runAs = new User(randomAsciiOfLengthBetween(4, 30),
                randomBoolean() ? generateRandomStringArray(20, 30, false) : null);
        User user = new User(randomAsciiOfLengthBetween(4, 30),
                generateRandomStringArray(20, 30, false), runAs);
        BytesStreamOutput output = new BytesStreamOutput();

        User.writeTo(user, output);
        User readFrom = User.readFrom(ByteBufferStreamInput.wrap(output.bytes()));

        assertThat(readFrom, not(sameInstance(user)));
        assertThat(readFrom.principal(), is(user.principal()));
        assertThat(Arrays.equals(readFrom.roles(), user.roles()), is(true));
        assertThat(readFrom.runAs(), is(notNullValue()));
        User readFromRunAs = readFrom.runAs();
        assertThat(readFromRunAs.principal(), is(runAs.principal()));
        assertThat(Arrays.equals(readFromRunAs.roles(), runAs.roles()), is(true));
        assertThat(readFromRunAs.runAs(), is(nullValue()));
    }

    public void testSystemReadAndWrite() throws Exception {
        BytesStreamOutput output = new BytesStreamOutput();

        User.writeTo(SystemUser.INSTANCE, output);
        User readFrom = User.readFrom(ByteBufferStreamInput.wrap(output.bytes()));

        assertThat(readFrom, is(sameInstance(SystemUser.INSTANCE)));
        assertThat(readFrom.runAs(), is(nullValue()));
    }

    public void testInternalShieldUserReadAndWrite() throws Exception {
        BytesStreamOutput output = new BytesStreamOutput();

        User.writeTo(XPackUser.INSTANCE, output);
        User readFrom = User.readFrom(ByteBufferStreamInput.wrap(output.bytes()));

        assertThat(readFrom, is(sameInstance(XPackUser.INSTANCE)));
    }

    public void testFakeInternalUserSerialization() throws Exception {
        BytesStreamOutput output = new BytesStreamOutput();
        output.writeBoolean(true);
        output.writeString(randomAsciiOfLengthBetween(4, 30));
        try {
            User.readFrom(ByteBufferStreamInput.wrap(output.bytes()));
            fail("system user had wrong name");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    public void testCreateUserRunningAsSystemUser() throws Exception {
        try {
            new User(randomAsciiOfLengthBetween(3, 10),
                    generateRandomStringArray(16, 30, false), SystemUser.INSTANCE);
            fail("should not be able to create a runAs user with the system user");
        } catch (ElasticsearchSecurityException e) {
            assertThat(e.getMessage(), containsString("system"));
        }
    }

    public void testUserToString() throws Exception {
        User sudo = new User("root", new String[]{"r3"});
        User u = new User("bob", new String[]{"r1", "r2"}, sudo);
        assertEquals("User[username=root,roles=[r3,]]", sudo.toString());
        assertEquals("User[username=bob,roles=[r1,r2,],runAs=[User[username=root,roles=[r3,]]]]",
                u.toString());
    }
}
