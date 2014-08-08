package org.elasticsearch.shield.audit;

import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.User;
import org.elasticsearch.shield.authc.AuthenticationToken;
import org.elasticsearch.transport.TransportRequest;

import java.util.Set;

/**
 *
 */
public class AuditTrailService extends AbstractComponent implements AuditTrail {

    private final AuditTrail[] auditTrails;

    @Inject
    public AuditTrailService(Settings settings, Set<AuditTrail> auditTrails) {
        super(settings);
        this.auditTrails = auditTrails.toArray(new AuditTrail[auditTrails.size()]);
    }

    @Override
    public void anonymousAccess(String action, TransportRequest request) {
        for (int i = 0; i < auditTrails.length; i++) {
            auditTrails[i].anonymousAccess(action, request);
        }
    }

    @Override
    public void authenticationFailed(String realm, AuthenticationToken token, String action, TransportRequest request) {
        for (int i = 0; i < auditTrails.length; i++) {
            auditTrails[i].authenticationFailed(realm, token, action, request);
        }
    }

    @Override
    public void accessGranted(User user, String action, TransportRequest request) {
        for (int i = 0; i < auditTrails.length; i++) {
            auditTrails[i].accessGranted(user, action, request);
        }
    }

    @Override
    public void accessDenied(User user, String action, TransportRequest request) {
        for (int i = 0; i < auditTrails.length; i++) {
            auditTrails[i].accessDenied(user, action, request);
        }
    }

}
