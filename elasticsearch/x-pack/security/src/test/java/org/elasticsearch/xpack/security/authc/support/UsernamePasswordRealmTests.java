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

package org.elasticsearch.xpack.security.authc.support;

import org.elasticsearch.test.ESTestCase;

import java.util.Locale;

import static org.hamcrest.core.Is.is;

/**
 *
 */
public class UsernamePasswordRealmTests extends ESTestCase {

    public void testUserbaseScaelResolve() throws Exception {
        int count = randomIntBetween(0, 1000);
        UsernamePasswordRealm.UserbaseSize size = UsernamePasswordRealm.UserbaseSize.resolve(count);
        if (count < 10) {
            assertThat(size, is(UsernamePasswordRealm.UserbaseSize.TINY));
        } else if (count < 100) {
            assertThat(size, is(UsernamePasswordRealm.UserbaseSize.SMALL));
        } else if (count < 500) {
            assertThat(size, is(UsernamePasswordRealm.UserbaseSize.MEDIUM));
        } else if (count < 1000) {
            assertThat(size, is(UsernamePasswordRealm.UserbaseSize.LARGE));
        } else {
            assertThat(size, is(UsernamePasswordRealm.UserbaseSize.XLARGE));
        }
    }

    public void testUserbaseScaleToString() throws Exception {
        UsernamePasswordRealm.UserbaseSize size = randomFrom(UsernamePasswordRealm.UserbaseSize.values());
        String value = size.toString();
        if (size == UsernamePasswordRealm.UserbaseSize.XLARGE) {
            assertThat(value , is("x-large"));
        } else {
            assertThat(value , is(size.name().toLowerCase(Locale.ROOT)));
        }
    }
}
