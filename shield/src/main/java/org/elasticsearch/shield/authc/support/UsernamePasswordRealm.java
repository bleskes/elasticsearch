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

import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.shield.authc.AuthenticationToken;
import org.elasticsearch.shield.authc.Realm;
import org.elasticsearch.shield.authc.RealmConfig;
import org.elasticsearch.transport.TransportMessage;

import static org.elasticsearch.shield.authc.support.UsernamePasswordToken.BASIC_AUTH_HEADER;

/**
 *
 */
public abstract class UsernamePasswordRealm extends Realm<UsernamePasswordToken> {

    public UsernamePasswordRealm(String type, RealmConfig config) {
        super(type, config);
    }

    @Override
    public UsernamePasswordToken token(RestRequest request) {
        return UsernamePasswordToken.extractToken(request, null);
    }

    @Override
    public UsernamePasswordToken token(TransportMessage<?> message) {
        return UsernamePasswordToken.extractToken(message, null);
    }

    public boolean supports(AuthenticationToken token) {
        return token instanceof UsernamePasswordToken;
    }

    public static abstract class Factory<R extends UsernamePasswordRealm> extends Realm.Factory<R> {

        protected Factory(String type, boolean internal, RestController restController) {
            super(type, internal);
            restController.registerRelevantHeaders(BASIC_AUTH_HEADER);
        }
    }
}
