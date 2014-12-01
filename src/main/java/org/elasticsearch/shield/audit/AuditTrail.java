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

package org.elasticsearch.shield.audit;

import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.shield.User;
import org.elasticsearch.shield.authc.AuthenticationToken;
import org.elasticsearch.shield.transport.n2n.ProfileIpFilterRule;
import org.elasticsearch.transport.TransportMessage;
import org.elasticsearch.transport.TransportRequest;

import java.net.InetAddress;

/**
 *
 */
public interface AuditTrail {

    static final AuditTrail NOOP = new AuditTrail() {

        static final String NAME = "noop";

        @Override
        public String name() {
            return NAME;
        }

        @Override
        public void anonymousAccess(String action, TransportMessage<?> message) {
        }

        @Override
        public void anonymousAccess(RestRequest request) {
        }

        @Override
        public void authenticationFailed(AuthenticationToken token, String action, TransportMessage<?> message) {
        }

        @Override
        public void authenticationFailed(AuthenticationToken token, RestRequest request) {
        }

        @Override
        public void authenticationFailed(String realm, AuthenticationToken token, String action, TransportMessage<?> message) {
        }

        @Override
        public void authenticationFailed(String realm, AuthenticationToken token, RestRequest request) {
        }

        @Override
        public void accessGranted(User user, String action, TransportMessage<?> message) {
        }

        @Override
        public void accessDenied(User user, String action, TransportMessage<?> message) {
        }

        @Override
        public void tamperedRequest(User user, String action, TransportRequest request) {
        }

        @Override
        public void connectionGranted(InetAddress inetAddress, ProfileIpFilterRule rule) {
        }

        @Override
        public void connectionDenied(InetAddress inetAddress, ProfileIpFilterRule rule) {
        }
    };

    String name();

    void anonymousAccess(String action, TransportMessage<?> message);

    void anonymousAccess(RestRequest request);

    void authenticationFailed(AuthenticationToken token, String action, TransportMessage<?> message);

    void authenticationFailed(AuthenticationToken token, RestRequest request);

    void authenticationFailed(String realm, AuthenticationToken token, String action, TransportMessage<?> message);

    void authenticationFailed(String realm, AuthenticationToken token, RestRequest request);

    void accessGranted(User user, String action, TransportMessage<?> message);

    void accessDenied(User user, String action, TransportMessage<?> message);

    void tamperedRequest(User user, String action, TransportRequest request);

    void connectionGranted(InetAddress inetAddress, ProfileIpFilterRule rule);

    void connectionDenied(InetAddress inetAddress, ProfileIpFilterRule rule);
}
