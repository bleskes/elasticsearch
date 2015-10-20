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

package org.elasticsearch.shield.transport;

import org.elasticsearch.ElasticsearchSecurityException;
import org.elasticsearch.shield.User;
import org.elasticsearch.shield.action.ShieldActionMapper;
import org.elasticsearch.shield.authc.AuthenticationService;
import org.elasticsearch.shield.authz.AuthorizationService;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.transport.TransportRequest;
import org.elasticsearch.transport.netty.NettyTransport;
import org.elasticsearch.transport.netty.NettyTransportChannel;
import org.junit.Before;

import static org.elasticsearch.shield.support.Exceptions.authenticationError;
import static org.elasticsearch.shield.support.Exceptions.authorizationError;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 *
 */
public class ServerTransportFilterTests extends ESTestCase {
    private AuthenticationService authcService;
    private AuthorizationService authzService;
    private ServerTransportFilter filter;
    private NettyTransportChannel channel;

    @Before
    public void init() throws Exception {
        authcService = mock(AuthenticationService.class);
        authzService = mock(AuthorizationService.class);
        channel = mock(NettyTransportChannel.class);
        when(channel.getProfileName()).thenReturn(NettyTransport.DEFAULT_PROFILE);
        filter = new ServerTransportFilter.NodeProfile(authcService, authzService, new ShieldActionMapper(), false);
    }

    public void testInbound() throws Exception {
        TransportRequest request = mock(TransportRequest.class);
        User user = mock(User.class);
        when(authcService.authenticate("_action", request, null)).thenReturn(user);
        filter.inbound("_action", request, channel);
        verify(authzService).authorize(user, "_action", request);
    }

    public void testInboundAuthenticationException() throws Exception {
        TransportRequest request = mock(TransportRequest.class);
        doThrow(authenticationError("authc failed")).when(authcService).authenticate("_action", request, null);
        try {
            filter.inbound("_action", request, channel);
            fail("expected filter inbound to throw an authentication exception on authentication error");
        } catch (ElasticsearchSecurityException e) {
            assertThat(e.getMessage(), equalTo("authc failed"));
        }
        verifyZeroInteractions(authzService);
    }

    public void testInboundAuthorizationException() throws Exception {
        TransportRequest request = mock(TransportRequest.class);
        User user = mock(User.class);
        when(authcService.authenticate("_action", request, null)).thenReturn(user);
        doThrow(authorizationError("authz failed")).when(authzService).authorize(user, "_action", request);
        try {
            filter.inbound("_action", request, channel);
            fail("expected filter inbound to throw an authorization exception on authorization error");
        } catch (ElasticsearchSecurityException e) {
            assertThat(e.getMessage(), equalTo("authz failed"));
        }
    }
}
