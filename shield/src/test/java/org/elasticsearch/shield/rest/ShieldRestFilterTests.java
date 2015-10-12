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

package org.elasticsearch.shield.rest;

import org.elasticsearch.ElasticsearchSecurityException;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestFilterChain;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.shield.User;
import org.elasticsearch.shield.authc.AuthenticationService;
import org.elasticsearch.shield.license.ShieldLicenseState;
import org.elasticsearch.test.ESTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.elasticsearch.shield.support.Exceptions.authenticationError;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

/**
 *
 */
public class ShieldRestFilterTests extends ESTestCase {

    private AuthenticationService authcService;
    private RestChannel channel;
    private RestFilterChain chain;
    private ShieldRestFilter filter;
    private ShieldLicenseState licenseState;

    @Before
    public void init() throws Exception {
        authcService = mock(AuthenticationService.class);
        RestController restController = mock(RestController.class);
        channel = mock(RestChannel.class);
        chain = mock(RestFilterChain.class);
        licenseState = mock(ShieldLicenseState.class);
        when(licenseState.securityEnabled()).thenReturn(true);
        filter = new ShieldRestFilter(authcService, restController, Settings.EMPTY, licenseState);
        verify(restController).registerFilter(filter);
    }

    @Test
    public void testProcess() throws Exception {
        RestRequest request = mock(RestRequest.class);
        User user = new User.Simple("_user", new String[] { "r1" });
        when(authcService.authenticate(request)).thenReturn(user);
        filter.process(request, channel, chain);
        verify(chain).continueProcessing(request, channel);
        verifyZeroInteractions(channel);
    }

    @Test
    public void testProcessBasicLicense() throws Exception {
        RestRequest request = mock(RestRequest.class);
        when(licenseState.securityEnabled()).thenReturn(false);
        filter.process(request, channel, chain);
        verify(chain).continueProcessing(request, channel);
        verifyZeroInteractions(channel, authcService);
    }

    @Test
    public void testProcess_AuthenticationError() throws Exception {
        RestRequest request = mock(RestRequest.class);
        when(authcService.authenticate(request)).thenThrow(authenticationError("failed authc"));
        try {
            filter.process(request, channel, chain);
            fail("expected rest filter process to throw an authentication exception when authentication fails");
        } catch (ElasticsearchSecurityException e) {
            assertThat(e.getMessage(), equalTo("failed authc"));
        }
        verifyZeroInteractions(channel);
        verifyZeroInteractions(chain);
    }

    @Test
    public void testProcess_OptionsMethod() throws Exception {
        RestRequest request = mock(RestRequest.class);
        when(request.method()).thenReturn(RestRequest.Method.OPTIONS);
        filter.process(request, channel, chain);
        verify(chain).continueProcessing(request, channel);
        verifyZeroInteractions(channel);
        verifyZeroInteractions(authcService);
    }
}
