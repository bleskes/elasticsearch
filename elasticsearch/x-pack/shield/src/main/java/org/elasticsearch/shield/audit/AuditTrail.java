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
import org.elasticsearch.shield.user.User;
import org.elasticsearch.shield.authc.AuthenticationToken;
import org.elasticsearch.shield.transport.filter.ShieldIpFilterRule;
import org.elasticsearch.transport.TransportMessage;

import java.net.InetAddress;

/**
 *
 */
public interface AuditTrail {

    AuditTrail NOOP = new AuditTrail() {

        static final String NAME = "noop";

        @Override
        public String name() {
            return NAME;
        }

        @Override
        public void anonymousAccessDenied(String action, TransportMessage message) {
        }

        @Override
        public void anonymousAccessDenied(RestRequest request) {
        }

        @Override
        public void authenticationFailed(RestRequest request) {
        }

        @Override
        public void authenticationFailed(String action, TransportMessage message) {
        }

        @Override
        public void authenticationFailed(AuthenticationToken token, String action, TransportMessage message) {
        }

        @Override
        public void authenticationFailed(AuthenticationToken token, RestRequest request) {
        }

        @Override
        public void authenticationFailed(String realm, AuthenticationToken token, String action, TransportMessage message) {
        }

        @Override
        public void authenticationFailed(String realm, AuthenticationToken token, RestRequest request) {
        }

        @Override
        public void accessGranted(User user, String action, TransportMessage message) {
        }

        @Override
        public void accessDenied(User user, String action, TransportMessage message) {
        }

        @Override
        public void tamperedRequest(RestRequest request) {
        }

        @Override
        public void tamperedRequest(String action, TransportMessage message) {
        }

        @Override
        public void tamperedRequest(User user, String action, TransportMessage request) {
        }

        @Override
        public void connectionGranted(InetAddress inetAddress, String profile, ShieldIpFilterRule rule) {
        }

        @Override
        public void connectionDenied(InetAddress inetAddress, String profile, ShieldIpFilterRule rule) {
        }

        @Override
        public void runAsGranted(User user, String action, TransportMessage message) {
        }

        @Override
        public void runAsDenied(User user, String action, TransportMessage message) {
        }
    };

    String name();

    void anonymousAccessDenied(String action, TransportMessage message);

    void anonymousAccessDenied(RestRequest request);

    void authenticationFailed(RestRequest request);

    void authenticationFailed(String action, TransportMessage message);

    void authenticationFailed(AuthenticationToken token, String action, TransportMessage message);

    void authenticationFailed(AuthenticationToken token, RestRequest request);

    void authenticationFailed(String realm, AuthenticationToken token, String action, TransportMessage message);

    void authenticationFailed(String realm, AuthenticationToken token, RestRequest request);

    void accessGranted(User user, String action, TransportMessage message);

    void accessDenied(User user, String action, TransportMessage message);

    void tamperedRequest(RestRequest request);

    void tamperedRequest(String action, TransportMessage message);

    void tamperedRequest(User user, String action, TransportMessage request);

    void connectionGranted(InetAddress inetAddress, String profile, ShieldIpFilterRule rule);

    void connectionDenied(InetAddress inetAddress, String profile, ShieldIpFilterRule rule);

    void runAsGranted(User user, String action, TransportMessage message);

    void runAsDenied(User user, String action, TransportMessage message);
}
