package org.elasticsearch.shield.audit;

import org.elasticsearch.shield.User;
import org.elasticsearch.shield.authc.AuthenticationToken;
import org.elasticsearch.transport.TransportRequest;

/**
 *
 */
public interface AuditTrail {

    public static final AuditTrail NOOP = new AuditTrail() {
        @Override
        public void anonymousAccess(String action, TransportRequest request) {
        }

        @Override
        public void authenticationFailed(AuthenticationToken token, String action, TransportRequest request) {
        }

        @Override
        public void accessGranted(User user, String action, TransportRequest request) {
        }

        @Override
        public void accessDenied(User user, String action, TransportRequest request) {
        }
    };

    void anonymousAccess(String action, TransportRequest request);

    void authenticationFailed(AuthenticationToken token, String action, TransportRequest request);

    void accessGranted(User user, String action, TransportRequest request);

    void accessDenied(User user, String action, TransportRequest request);

}
