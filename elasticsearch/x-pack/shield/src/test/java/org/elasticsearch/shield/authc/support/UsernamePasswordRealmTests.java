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

import org.elasticsearch.test.ESTestCase;

import java.util.Locale;

import static org.hamcrest.core.Is.is;

/**
 *
 */
public class UsernamePasswordRealmTests extends ESTestCase {

    public void testUserbaseScaelResolve() throws Exception {
        int count = randomIntBetween(0, 1000);
        UsernamePasswordRealm.UserbaseScale scale = UsernamePasswordRealm.UserbaseScale.resolve(count);
        if (count < 10) {
            assertThat(scale, is(UsernamePasswordRealm.UserbaseScale.SMALL));
        } else if (count < 50) {
            assertThat(scale, is(UsernamePasswordRealm.UserbaseScale.MEDIUM));
        } else if (count < 250) {
            assertThat(scale, is(UsernamePasswordRealm.UserbaseScale.LARGE));
        } else {
            assertThat(scale, is(UsernamePasswordRealm.UserbaseScale.XLARGE));
        }
    }

    public void testUserbaseScaleToString() throws Exception {
        UsernamePasswordRealm.UserbaseScale scale = randomFrom(UsernamePasswordRealm.UserbaseScale.values());
        String value = scale.toString();
        if (scale == UsernamePasswordRealm.UserbaseScale.XLARGE) {
            assertThat(value , is("x-large"));
        } else {
            assertThat(value , is(scale.name().toLowerCase(Locale.ROOT)));
        }
    }
}
