/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2017] Elasticsearch Incorporated. All Rights Reserved.
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

package org.elasticsearch.xpack.security.authc;

import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.security.authc.Authentication.RealmRef;
import org.elasticsearch.xpack.security.user.User;

import java.io.IOException;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class UserTokenTests extends ESTestCase {

    public void testSerialization() throws IOException {
        final Authentication authentication = new Authentication(new User("joe", "a role"), new RealmRef("realm", "native", "node1"), null);
        final int seconds = randomIntBetween(0, Math.toIntExact(TimeValue.timeValueMinutes(30L).getSeconds()));
        final ZonedDateTime expirationTime = Clock.systemUTC().instant().atZone(ZoneOffset.UTC).plusSeconds(seconds);
        final UserToken userToken = new UserToken(authentication, expirationTime);

        BytesStreamOutput output = new BytesStreamOutput();
        userToken.writeTo(output);

        final UserToken serialized = new UserToken(output.bytes().streamInput());
        assertEquals(authentication, serialized.getAuthentication());
        assertEquals(expirationTime, serialized.getExpirationTime());
    }
}
