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

import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.shield.authc.AuthenticationToken;
import org.elasticsearch.shield.authc.Realm;
import org.elasticsearch.shield.authc.RealmConfig;

import java.util.Locale;

/**
 *
 */
public abstract class UsernamePasswordRealm extends Realm<UsernamePasswordToken> {

    public UsernamePasswordRealm(String type, RealmConfig config) {
        super(type, config);
    }

    @Override
    public UsernamePasswordToken token(ThreadContext threadContext) {
        return UsernamePasswordToken.extractToken(threadContext);
    }

    public boolean supports(AuthenticationToken token) {
        return token instanceof UsernamePasswordToken;
    }

    public static abstract class Factory<R extends UsernamePasswordRealm> extends Realm.Factory<R> {

        protected Factory(String type, RestController restController, boolean internal) {
            super(type, internal);
            restController.registerRelevantHeaders(UsernamePasswordToken.BASIC_AUTH_HEADER);
        }
    }

    public enum UserbaseScale {

        SMALL,
        MEDIUM,
        LARGE,
        XLARGE;

        public static UserbaseScale resolve(int count) {
            if (count < 10) {
                return SMALL;
            }
            if (count < 50) {
                return MEDIUM;
            }
            if (count < 250) {
                return LARGE;
            }
            return XLARGE;
        }

        @Override
        public String toString() {
            return this == XLARGE ? "x-large" : name().toLowerCase(Locale.ROOT);
        }
    }
}
