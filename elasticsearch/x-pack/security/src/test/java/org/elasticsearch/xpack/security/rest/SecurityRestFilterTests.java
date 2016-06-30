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

package org.elasticsearch.xpack.security.rest;

import org.elasticsearch.ElasticsearchSecurityException;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestFilterChain;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.xpack.security.authc.Authentication;
import org.elasticsearch.xpack.security.authc.AuthenticationService;
import org.elasticsearch.xpack.security.SecurityLicenseState;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.threadpool.ThreadPool;
import org.junit.Before;

import static org.elasticsearch.xpack.security.support.Exceptions.authenticationError;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 *
 */
public class SecurityRestFilterTests extends ESTestCase {
    private AuthenticationService authcService;
    private RestChannel channel;
    private RestFilterChain chain;
    private SecurityRestFilter filter;
    private SecurityLicenseState licenseState;

    @Before
    public void init() throws Exception {
        authcService = mock(AuthenticationService.class);
        RestController restController = mock(RestController.class);
        channel = mock(RestChannel.class);
        chain = mock(RestFilterChain.class);
        licenseState = mock(SecurityLicenseState.class);
        when(licenseState.authenticationAndAuthorizationEnabled()).thenReturn(true);
        ThreadPool threadPool = mock(ThreadPool.class);
        when(threadPool.getThreadContext()).thenReturn(new ThreadContext(Settings.EMPTY));
        filter = new SecurityRestFilter(authcService, restController, Settings.EMPTY, threadPool, licenseState);
        verify(restController).registerFilter(filter);
    }

    public void testProcess() throws Exception {
        RestRequest request = mock(RestRequest.class);
        Authentication authentication = mock(Authentication.class);
        when(authcService.authenticate(request)).thenReturn(authentication);
        filter.process(request, channel, null, chain);
        verify(chain).continueProcessing(request, channel, null);
        verifyZeroInteractions(channel);
    }

    public void testProcessBasicLicense() throws Exception {
        RestRequest request = mock(RestRequest.class);
        when(licenseState.authenticationAndAuthorizationEnabled()).thenReturn(false);
        filter.process(request, channel, null, chain);
        verify(chain).continueProcessing(request, channel, null);
        verifyZeroInteractions(channel, authcService);
    }

    public void testProcessAuthenticationError() throws Exception {
        RestRequest request = mock(RestRequest.class);
        when(authcService.authenticate(request)).thenThrow(authenticationError("failed authc"));
        try {
            filter.process(request, channel, null, chain);
            fail("expected rest filter process to throw an authentication exception when authentication fails");
        } catch (ElasticsearchSecurityException e) {
            assertThat(e.getMessage(), equalTo("failed authc"));
        }
        verifyZeroInteractions(channel);
        verifyZeroInteractions(chain);
    }

    public void testProcessOptionsMethod() throws Exception {
        RestRequest request = mock(RestRequest.class);
        when(request.method()).thenReturn(RestRequest.Method.OPTIONS);
        filter.process(request, channel, null, chain);
        verify(chain).continueProcessing(request, channel, null);
        verifyZeroInteractions(channel);
        verifyZeroInteractions(authcService);
    }
}
