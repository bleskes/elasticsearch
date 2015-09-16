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

import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.shield.User;
import org.elasticsearch.shield.authc.AuthenticationToken;
import org.elasticsearch.shield.transport.filter.ShieldIpFilterRule;
import org.elasticsearch.transport.Transport;
import org.elasticsearch.transport.TransportMessage;
import org.elasticsearch.transport.TransportRequest;

import java.net.InetAddress;
import java.util.Set;

/**
 *
 */
public class AuditTrailService extends AbstractComponent implements AuditTrail {

    final AuditTrail[] auditTrails;

    @Override
    public String name() {
        return "service";
    }

    @Inject
    public AuditTrailService(Settings settings, Set<AuditTrail> auditTrails) {
        super(settings);
        this.auditTrails = auditTrails.toArray(new AuditTrail[auditTrails.size()]);
    }

    @Override
    public void anonymousAccessDenied(String action, TransportMessage<?> message) {
        for (AuditTrail auditTrail : auditTrails) {
            auditTrail.anonymousAccessDenied(action, message);
        }
    }

    @Override
    public void anonymousAccessDenied(RestRequest request) {
        for (AuditTrail auditTrail : auditTrails) {
            auditTrail.anonymousAccessDenied(request);
        }
    }

    @Override
    public void authenticationFailed(RestRequest request) {
        for (AuditTrail auditTrail : auditTrails) {
            auditTrail.authenticationFailed(request);
        }
    }

    @Override
    public void authenticationFailed(String action, TransportMessage<?> message) {
        for (AuditTrail auditTrail : auditTrails) {
            auditTrail.authenticationFailed(action, message);
        }
    }

    @Override
    public void authenticationFailed(AuthenticationToken token, String action, TransportMessage<?> message) {
        for (AuditTrail auditTrail : auditTrails) {
            auditTrail.authenticationFailed(token, action, message);
        }
    }

    @Override
    public void authenticationFailed(String realm, AuthenticationToken token, String action, TransportMessage<?> message) {
        for (AuditTrail auditTrail : auditTrails) {
            auditTrail.authenticationFailed(realm, token, action, message);
        }
    }

    @Override
    public void authenticationFailed(AuthenticationToken token, RestRequest request) {
        for (AuditTrail auditTrail : auditTrails) {
            auditTrail.authenticationFailed(token, request);
        }
    }

    @Override
    public void authenticationFailed(String realm, AuthenticationToken token, RestRequest request) {
        for (AuditTrail auditTrail : auditTrails) {
            auditTrail.authenticationFailed(realm, token, request);
        }
    }

    @Override
    public void accessGranted(User user, String action, TransportMessage<?> message) {
        for (AuditTrail auditTrail : auditTrails) {
            auditTrail.accessGranted(user, action, message);
        }
    }

    @Override
    public void accessDenied(User user, String action, TransportMessage<?> message) {
        for (AuditTrail auditTrail : auditTrails) {
            auditTrail.accessDenied(user, action, message);
        }
    }

    @Override
    public void tamperedRequest(User user, String action, TransportRequest request) {
        for (AuditTrail auditTrail : auditTrails) {
            auditTrail.tamperedRequest(user, action, request);
        }
    }

    @Override
    public void connectionGranted(InetAddress inetAddress, String profile, ShieldIpFilterRule rule) {
        for (AuditTrail auditTrail : auditTrails) {
            auditTrail.connectionGranted(inetAddress, profile, rule);
        }
    }

    @Override
    public void connectionDenied(InetAddress inetAddress, String profile, ShieldIpFilterRule rule) {
        for (AuditTrail auditTrail : auditTrails) {
            auditTrail.connectionDenied(inetAddress, profile, rule);
        }
    }

    @Override
    public void runAsGranted(User user, String action, TransportMessage<?> message) {
        for (AuditTrail auditTrail : auditTrails) {
            auditTrail.runAsGranted(user, action, message);
        }
    }

    @Override
    public void runAsDenied(User user, String action, TransportMessage<?> message) {
        for (AuditTrail auditTrail : auditTrails) {
            auditTrail.runAsDenied(user, action, message);
        }
    }
}
