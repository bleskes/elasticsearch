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

package org.elasticsearch.shield.authc.system;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.shield.User;
import org.elasticsearch.shield.authc.AuthenticationToken;
import org.elasticsearch.shield.authc.Realm;
import org.elasticsearch.shield.support.AbstractShieldModule;
import org.elasticsearch.transport.TransportMessage;

/**
 *
 */
public class SystemRealm implements Realm<AuthenticationToken> {

    public static final AuthenticationToken TOKEN = new AuthenticationToken() {
        @Override
        public String principal() {
            return "_system";
        }

        @Override
        public Object credentials() {
            return null;
        }

        @Override
        public void clearCredentials() {

        }
    };

    @Override
    public String type() {
        return "system";
    }

    @Override
    public AuthenticationToken token(RestRequest request) {
        return null; // system token can never come from the rest API
    }

    @Override
    public AuthenticationToken token(TransportMessage<?> message) {
        // as far as this realm is concerned, there's never a system token
        // in the request. The decision of whether a request is a system
        // request or not, is made elsewhere where the system token is
        // assumed
        return null;
    }

    @Override
    public boolean supports(AuthenticationToken token) {
        return token == TOKEN;
    }

    @Override
    public User authenticate(AuthenticationToken token) {
        return token == TOKEN ? User.SYSTEM : null;
    }

    public static class Module extends AbstractShieldModule.Node {

        public Module(Settings settings) {
            super(settings);
        }

        @Override
        protected void configureNode() {
            bind(SystemRealm.class).asEagerSingleton();
        }
    }
}
