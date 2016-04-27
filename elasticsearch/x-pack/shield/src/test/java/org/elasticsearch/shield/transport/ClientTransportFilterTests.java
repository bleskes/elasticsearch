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

import org.elasticsearch.shield.user.SystemUser;
import org.elasticsearch.shield.authc.AuthenticationService;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.transport.TransportRequest;
import org.junit.Before;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 *
 */
public class ClientTransportFilterTests extends ESTestCase {
    private AuthenticationService authcService;
    private ClientTransportFilter filter;

    @Before
    public void init() throws Exception {
        authcService = mock(AuthenticationService.class);
        filter = new ClientTransportFilter.Node(authcService);
    }

    public void testOutbound() throws Exception {
        TransportRequest request = mock(TransportRequest.class);
        filter.outbound("_action", request);
        verify(authcService).attachUserIfMissing(SystemUser.INSTANCE);
    }
}
