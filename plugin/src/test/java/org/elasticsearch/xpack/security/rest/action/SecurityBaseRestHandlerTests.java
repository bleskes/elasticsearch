/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2017] Elasticsearch Incorporated. All Rights Reserved.
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

package org.elasticsearch.xpack.security.rest.action;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.test.rest.FakeRestChannel;
import org.elasticsearch.test.rest.FakeRestRequest;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class SecurityBaseRestHandlerTests extends ESTestCase {

    public void testSecurityBaseRestHandlerChecksLicenseState() throws Exception {
        final boolean securityEnabled = randomBoolean();
        final AtomicBoolean consumerCalled = new AtomicBoolean(false);
        final XPackLicenseState licenseState = mock(XPackLicenseState.class);
        when(licenseState.isAuthAllowed()).thenReturn(securityEnabled);
        SecurityBaseRestHandler handler = new SecurityBaseRestHandler(Settings.EMPTY, licenseState) {
            @Override
            protected RestChannelConsumer innerPrepareRequest(RestRequest request, NodeClient client) throws IOException {
                return channel -> {
                    if (consumerCalled.compareAndSet(false, true) == false) {
                        fail("consumerCalled was not false");
                    }
                };
            }
        };
        FakeRestRequest fakeRestRequest = new FakeRestRequest();
        FakeRestChannel fakeRestChannel = new FakeRestChannel(fakeRestRequest, randomBoolean(), securityEnabled ? 0 : 1);
        NodeClient client = mock(NodeClient.class);

        assertFalse(consumerCalled.get());
        verifyZeroInteractions(licenseState);
        handler.handleRequest(fakeRestRequest, fakeRestChannel, client);

        verify(licenseState).isAuthAllowed();
        if (securityEnabled) {
            assertTrue(consumerCalled.get());
            assertEquals(0, fakeRestChannel.responses().get());
            assertEquals(0, fakeRestChannel.errors().get());
        } else {
            assertFalse(consumerCalled.get());
            assertEquals(0, fakeRestChannel.responses().get());
            assertEquals(1, fakeRestChannel.errors().get());
        }
    }
}
