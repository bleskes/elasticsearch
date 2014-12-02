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

import com.google.common.collect.ImmutableSet;
import org.elasticsearch.common.netty.handler.ipfilter.PatternRule;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.shield.User;
import org.elasticsearch.shield.authc.AuthenticationToken;
import org.elasticsearch.shield.transport.filter.ProfileIpFilterRule;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.elasticsearch.transport.TransportMessage;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 *
 */
public class AuditTrailServiceTests extends ElasticsearchTestCase {

    private Set<AuditTrail> auditTrails;
    private AuditTrailService service;

    private AuthenticationToken token;
    private TransportMessage message;
    private RestRequest restRequest;

    @Before
    public void init() throws Exception {
        ImmutableSet.Builder<AuditTrail> builder = ImmutableSet.builder();
        for (int i = 0; i < randomIntBetween(1, 4); i++) {
            builder.add(mock(AuditTrail.class));
        }
        auditTrails = builder.build();
        service = new AuditTrailService(ImmutableSettings.EMPTY, auditTrails);
        token = mock(AuthenticationToken.class);
        message = mock(TransportMessage.class);
        restRequest = mock(RestRequest.class);
    }

    @Test
    public void testAuthenticationFailed() throws Exception {
        service.authenticationFailed(token, "_action", message);
        for (AuditTrail auditTrail : auditTrails) {
            verify(auditTrail).authenticationFailed(token, "_action", message);
        }
    }

    @Test
    public void testAuthenticationFailed_Rest() throws Exception {
        service.authenticationFailed(token, restRequest);
        for (AuditTrail auditTrail : auditTrails) {
            verify(auditTrail).authenticationFailed(token, restRequest);
        }
    }

    @Test
    public void testAuthenticationFailed_Realm() throws Exception {
        service.authenticationFailed("_realm", token, "_action", message);
        for (AuditTrail auditTrail : auditTrails) {
            verify(auditTrail).authenticationFailed("_realm", token, "_action", message);
        }
    }

    @Test
    public void testAuthenticationFailed_Rest_Realm() throws Exception {
        service.authenticationFailed("_realm", token, restRequest);
        for (AuditTrail auditTrail : auditTrails) {
            verify(auditTrail).authenticationFailed("_realm", token, restRequest);
        }
    }

    @Test
    public void testAnonymousAccess() throws Exception {
        service.anonymousAccess("_action", message);
        for (AuditTrail auditTrail : auditTrails) {
            verify(auditTrail).anonymousAccess("_action", message);
        }
    }

    @Test
    public void testAccessGranted() throws Exception {
        User user = new User.Simple("_username", "r1");
        service.accessGranted(user, "_action", message);
        for (AuditTrail auditTrail : auditTrails) {
            verify(auditTrail).accessGranted(user, "_action", message);
        }
    }

    @Test
    public void testAccessDenied() throws Exception {
        User user = new User.Simple("_username", "r1");
        service.accessDenied(user, "_action", message);
        for (AuditTrail auditTrail : auditTrails) {
            verify(auditTrail).accessDenied(user, "_action", message);
        }
    }

    @Test
    public void testConnectionGranted() throws Exception {
        InetAddress inetAddress = InetAddress.getLocalHost();
        ProfileIpFilterRule rule = new ProfileIpFilterRule("client", new PatternRule(true, "i:*"), "all");
        service.connectionGranted(inetAddress, rule);
        for (AuditTrail auditTrail : auditTrails) {
            verify(auditTrail).connectionGranted(inetAddress, rule);
        }
    }

    @Test
    public void testConnectionDenied() throws Exception {
        InetAddress inetAddress = InetAddress.getLocalHost();
        ProfileIpFilterRule rule = new ProfileIpFilterRule("client", new PatternRule(false, "i:*"), "all");
        service.connectionDenied(inetAddress, rule);
        for (AuditTrail auditTrail : auditTrails) {
            verify(auditTrail).connectionDenied(inetAddress, rule);
        }
    }
}
