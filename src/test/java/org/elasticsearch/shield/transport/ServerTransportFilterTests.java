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

import org.elasticsearch.shield.User;
import org.elasticsearch.shield.authc.AuthenticationException;
import org.elasticsearch.shield.authc.AuthenticationService;
import org.elasticsearch.shield.authz.AuthorizationException;
import org.elasticsearch.shield.authz.AuthorizationService;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.elasticsearch.transport.TransportRequest;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

/**
 *
 */
public class ServerTransportFilterTests extends ElasticsearchTestCase {

    private AuthenticationService authcService;
    private AuthorizationService authzService;
    private ServerTransportFilter filter;

    @Before
    public void init() throws Exception {
        authcService = mock(AuthenticationService.class);
        authzService = mock(AuthorizationService.class);
        filter = new ServerTransportFilter.Node(authcService, authzService);
    }

    @Test
    public void testInbound() throws Exception {
        TransportRequest request = mock(TransportRequest.class);
        User user = mock(User.class);
        when(authcService.authenticate("_action", request, null)).thenReturn(user);
        filter.inbound("_action", request);
        verify(authzService).authorize(user, "_action", request);
    }

    @Test
    public void testInbound_AuthenticationException() throws Exception {
        TransportRequest request = mock(TransportRequest.class);
        doThrow(new AuthenticationException("authc failed")).when(authcService).authenticate("_action", request, null);
        try {
            filter.inbound("_action", request);
            fail("expected filter inbound to throw an authentication exception on authentication error");
        } catch (AuthenticationException e) {
            assertThat(e.getMessage(), equalTo("authc failed"));
        }
        verifyZeroInteractions(authzService);
    }

    @Test
    public void testInbound_AuthorizationException() throws Exception {
        TransportRequest request = mock(TransportRequest.class);
        User user = mock(User.class);
        when(authcService.authenticate("_action", request, null)).thenReturn(user);
        doThrow(new AuthorizationException("authz failed")).when(authzService).authorize(user, "_action", request);
        try {
            filter.inbound("_action", request);
            fail("expected filter inbound to throw an authorization exception on authorization error");
        } catch (AuthorizationException e) {
            assertThat(e.getMessage(), equalTo("authz failed"));
        }
    }

}
