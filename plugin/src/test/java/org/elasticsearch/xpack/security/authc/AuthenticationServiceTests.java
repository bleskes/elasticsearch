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

package org.elasticsearch.xpack.security.authc;

import java.io.IOException;
import java.time.Clock;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.lucene.util.SetOnce;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchSecurityException;
import org.elasticsearch.Version;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.get.GetAction;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.support.PlainActionFuture;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.settings.SecureString;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.env.Environment;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.test.rest.FakeRestRequest;
import org.elasticsearch.threadpool.FixedExecutorBuilder;
import org.elasticsearch.threadpool.TestThreadPool;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportMessage;
import org.elasticsearch.xpack.XPackSettings;
import org.elasticsearch.xpack.security.InternalClient;
import org.elasticsearch.xpack.security.SecurityLifecycleService;
import org.elasticsearch.xpack.security.audit.AuditTrailService;
import org.elasticsearch.xpack.security.authc.Authentication.RealmRef;
import org.elasticsearch.xpack.security.authc.AuthenticationService.Authenticator;
import org.elasticsearch.xpack.security.authc.Realm.Factory;
import org.elasticsearch.xpack.security.authc.esnative.ReservedRealm;
import org.elasticsearch.xpack.security.authc.support.UsernamePasswordToken;
import org.elasticsearch.xpack.security.crypto.CryptoService;
import org.elasticsearch.xpack.security.user.AnonymousUser;
import org.elasticsearch.xpack.security.user.SystemUser;
import org.elasticsearch.xpack.security.user.User;
import org.junit.After;
import org.junit.Before;

import static org.elasticsearch.test.SecurityTestsUtils.assertAuthenticationException;
import static org.elasticsearch.xpack.security.support.Exceptions.authenticationError;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;


/**
 * Unit tests for the {@link AuthenticationService}
 */
public class AuthenticationServiceTests extends ESTestCase {

    public static final String SIGNING_PREFIX = "signed$";
    private AuthenticationService service;
    private TransportMessage message;
    private RestRequest restRequest;
    private Realms realms;
    private Realm firstRealm;
    private Realm secondRealm;
    private AuditTrailService auditTrail;
    private AuthenticationToken token;
    private CryptoService cryptoService;
    private ThreadPool threadPool;
    private ThreadContext threadContext;
    private TokenService tokenService;
    private SecurityLifecycleService lifecycleService;
    private Client client;
    private boolean useSsl;

    @Before
    public void init() throws Exception {
        token = mock(AuthenticationToken.class);
        message = new InternalMessage();
        restRequest = new FakeRestRequest();
        threadContext = new ThreadContext(Settings.EMPTY);

        useSsl = randomBoolean();

        firstRealm = mock(Realm.class);
        when(firstRealm.type()).thenReturn("file");
        when(firstRealm.name()).thenReturn("file_realm");
        secondRealm = mock(Realm.class);
        when(secondRealm.type()).thenReturn("second");
        when(secondRealm.name()).thenReturn("second_realm");
        Settings settings = Settings.builder()
                .put("path.home", createTempDir())
                .put("node.name", "authc_test")
                .put(XPackSettings.TOKEN_SERVICE_ENABLED_SETTING.getKey(), true)
                .put(XPackSettings.TRANSPORT_SSL_ENABLED.getKey(), useSsl)
                .build();
        XPackLicenseState licenseState = mock(XPackLicenseState.class);
        when(licenseState.allowedRealmType()).thenReturn(XPackLicenseState.AllowedRealmType.ALL);
        when(licenseState.isAuthAllowed()).thenReturn(true);
        realms = new TestRealms(Settings.EMPTY, new Environment(settings), Collections.<String, Realm.Factory>emptyMap(), licenseState,
                threadContext, mock(ReservedRealm.class), Arrays.asList(firstRealm, secondRealm), Collections.singletonList(firstRealm));
        cryptoService = mock(CryptoService.class);

        auditTrail = mock(AuditTrailService.class);
        client = mock(Client.class);
        threadPool = new ThreadPool(settings,
                new FixedExecutorBuilder(settings, TokenService.THREAD_POOL_NAME, 1, 1000, "xpack.security.authc.token.thread_pool"));
        threadContext = threadPool.getThreadContext();
        when(cryptoService.sign(any(String.class), any(Version.class))).thenAnswer(invocation -> {
            assert invocation.getArguments().length == 2;
            String toSign = (String) invocation.getArguments()[0];
            return SIGNING_PREFIX + toSign;
        });
        when(cryptoService.unsignAndVerify(any(String.class), any(Version.class))).thenAnswer(invocation -> {
            assert invocation.getArguments().length == 2;
            String signed = (String) invocation.getArguments()[0];
            if (signed.startsWith(SIGNING_PREFIX)) {
                return signed.substring(SIGNING_PREFIX.length());
            } else {
                return signed;
            }
        });
        InternalClient internalClient = new InternalClient(Settings.EMPTY, threadPool, client, cryptoService);
        lifecycleService = mock(SecurityLifecycleService.class);
        tokenService = new TokenService(settings, Clock.systemUTC(), internalClient, lifecycleService);
        service = new AuthenticationService(settings, realms, auditTrail, cryptoService,
                new DefaultAuthenticationFailureHandler(), threadPool, new AnonymousUser(settings), tokenService);
    }

    @After
    public void shutdownThreadpool() throws InterruptedException {
        if (threadPool != null) {
            terminate(threadPool);
        }
    }

    @SuppressWarnings("unchecked")
    public void testTokenFirstMissingSecondFound() throws Exception {
        when(firstRealm.token(threadContext)).thenReturn(null);
        when(secondRealm.token(threadContext)).thenReturn(token);

        PlainActionFuture<Authentication> future = new PlainActionFuture<>();
        Authenticator authenticator = service.createAuthenticator("_action", message, null, Version.CURRENT, future);
        authenticator.extractToken((result) -> {
            assertThat(result, notNullValue());
            assertThat(result, is(token));
            verifyZeroInteractions(auditTrail);
        });
    }

    public void testTokenMissing() throws Exception {
        PlainActionFuture<Authentication> future = new PlainActionFuture<>();
        Authenticator authenticator = service.createAuthenticator("_action", message, null, Version.CURRENT, future);
        authenticator.extractToken((token) -> {
            assertThat(token, nullValue());
            authenticator.handleNullToken();
        });

        ElasticsearchSecurityException e = expectThrows(ElasticsearchSecurityException.class, () -> future.actionGet());
        assertThat(e.getMessage(), containsString("missing authentication token"));
        verify(auditTrail).anonymousAccessDenied("_action", message);
        verifyNoMoreInteractions(auditTrail);
    }

    @SuppressWarnings("unchecked")
    public void testAuthenticateBothSupportSecondSucceeds() throws Exception {
        User user = new User("_username", "r1");
        when(firstRealm.supports(token)).thenReturn(true);
        mockAuthenticate(firstRealm, token, null);
        when(secondRealm.supports(token)).thenReturn(true);
        mockAuthenticate(secondRealm, token, user);
        if (randomBoolean()) {
            when(firstRealm.token(threadContext)).thenReturn(token);
        } else {
            when(secondRealm.token(threadContext)).thenReturn(token);
        }

        final AtomicBoolean completed = new AtomicBoolean(false);
        service.authenticate("_action", message, null, Version.CURRENT, ActionListener.wrap(result -> {
            assertThat(result, notNullValue());
            assertThat(result.getUser(), is(user));
            assertThat(result.getLookedUpBy(), is(nullValue()));
            assertThat(result.getAuthenticatedBy(), is(notNullValue())); // TODO implement equals
            assertThreadContextContainsAuthentication(result);
            setCompletedToTrue(completed);
        }, this::logAndFail));
        assertTrue(completed.get());
        verify(auditTrail).authenticationFailed(firstRealm.name(), token, "_action", message);
    }

    public void testAuthenticateFirstNotSupportingSecondSucceeds() throws Exception {
        User user = new User("_username", "r1");
        when(firstRealm.supports(token)).thenReturn(false);
        when(secondRealm.supports(token)).thenReturn(true);
        mockAuthenticate(secondRealm, token, user);
        when(secondRealm.token(threadContext)).thenReturn(token);

        final AtomicBoolean completed = new AtomicBoolean(false);
        service.authenticate("_action", message, null, Version.CURRENT, ActionListener.wrap(result -> {
            assertThat(result, notNullValue());
            assertThat(result.getUser(), is(user));
            assertThreadContextContainsAuthentication(result);
            setCompletedToTrue(completed);
        }, this::logAndFail));
        verify(auditTrail).authenticationSuccess(secondRealm.name(), user, "_action", message);
        verifyNoMoreInteractions(auditTrail);
        verify(firstRealm, never()).authenticate(eq(token), any(ActionListener.class));
        assertTrue(completed.get());
    }

    public void testAuthenticateCached() throws Exception {
        final Authentication authentication = new Authentication(new User("_username", "r1"), new RealmRef("test", "cached", "foo"), null);
        authentication.writeToContext(threadContext, cryptoService, Settings.EMPTY, Version.CURRENT, true);

        Authentication result = authenticateBlocking("_action", message, null);

        assertThat(result, notNullValue());
        assertThat(result, is(authentication));
        verifyZeroInteractions(auditTrail);
        verifyZeroInteractions(firstRealm);
        verifyZeroInteractions(secondRealm);
    }

    public void testAuthenticateNonExistentRestRequestUserThrowsAuthenticationException() throws Exception {
        when(firstRealm.token(threadContext)).thenReturn(new UsernamePasswordToken("idonotexist",
                new SecureString("passwd".toCharArray())));
        try {
            authenticateBlocking(restRequest);
            fail("Authentication was successful but should not");
        } catch (ElasticsearchSecurityException e) {
            assertAuthenticationException(e, containsString("unable to authenticate user [idonotexist] for REST request [/]"));
        }
    }

    public void testTokenRestMissing() throws Exception {
        when(firstRealm.token(threadContext)).thenReturn(null);
        when(secondRealm.token(threadContext)).thenReturn(null);

        Authenticator authenticator = service.createAuthenticator(restRequest, mock(ActionListener.class));
        authenticator.extractToken((token) -> {
            assertThat(token, nullValue());
        });
    }

    public void authenticationInContextAndHeader() throws Exception {
        User user = new User("_username", "r1");
        when(firstRealm.token(threadContext)).thenReturn(token);
        when(firstRealm.supports(token)).thenReturn(true);
        mockAuthenticate(firstRealm, token, user);

        Authentication result = authenticateBlocking("_action", message, null);

        assertThat(result, notNullValue());
        assertThat(result.getUser(), is(user));

        String userStr = threadContext.getHeader(Authentication.AUTHENTICATION_KEY);
        assertThat(userStr, notNullValue());
        assertThat(userStr, equalTo("_signed_auth"));

        Authentication ctxAuth = threadContext.getTransient(Authentication.AUTHENTICATION_KEY);
        assertThat(ctxAuth, is(result));
    }

    public void testAuthenticateTransportAnonymous() throws Exception {
        when(firstRealm.token(threadContext)).thenReturn(null);
        when(secondRealm.token(threadContext)).thenReturn(null);
        try {
            authenticateBlocking("_action", message, null);
            fail("expected an authentication exception when trying to authenticate an anonymous message");
        } catch (ElasticsearchSecurityException e) {
            // expected
            assertAuthenticationException(e);
        }
        verify(auditTrail).anonymousAccessDenied("_action", message);
    }

    public void testAuthenticateRestAnonymous()  throws Exception {
        when(firstRealm.token(threadContext)).thenReturn(null);
        when(secondRealm.token(threadContext)).thenReturn(null);
        try {
            authenticateBlocking(restRequest);
            fail("expected an authentication exception when trying to authenticate an anonymous message");
        } catch (ElasticsearchSecurityException e) {
            // expected
            assertAuthenticationException(e);
        }
        verify(auditTrail).anonymousAccessDenied(restRequest);
    }

    public void testAuthenticateTransportFallback() throws Exception {
        when(firstRealm.token(threadContext)).thenReturn(null);
        when(secondRealm.token(threadContext)).thenReturn(null);
        User user1 = new User("username", "r1", "r2");

        Authentication result = authenticateBlocking("_action", message, user1);
        assertThat(result, notNullValue());
        assertThat(result.getUser(), sameInstance(user1));
        assertThreadContextContainsAuthentication(result);
    }

    public void testAuthenticateTransportDisabledUser() throws Exception {
        User user = new User("username", new String[] { "r1", "r2" }, null, null, null, false);
        User fallback = randomBoolean() ? SystemUser.INSTANCE : null;
        when(firstRealm.token(threadContext)).thenReturn(token);
        when(firstRealm.supports(token)).thenReturn(true);
        mockAuthenticate(firstRealm, token, user);

        ElasticsearchSecurityException e =
                expectThrows(ElasticsearchSecurityException.class, () -> authenticateBlocking("_action", message, fallback));
        verify(auditTrail).authenticationFailed(token, "_action", message);
        verifyNoMoreInteractions(auditTrail);
        assertAuthenticationException(e);
    }

    public void testAuthenticateRestDisabledUser() throws Exception {
        User user = new User("username", new String[] { "r1", "r2" }, null, null, null, false);
        when(firstRealm.token(threadContext)).thenReturn(token);
        when(firstRealm.supports(token)).thenReturn(true);
        mockAuthenticate(firstRealm, token, user);

        ElasticsearchSecurityException e =
                expectThrows(ElasticsearchSecurityException.class, () -> authenticateBlocking(restRequest));
        verify(auditTrail).authenticationFailed(token, restRequest);
        verifyNoMoreInteractions(auditTrail);
        assertAuthenticationException(e);
    }

    public void testAuthenticateTransportSuccess() throws Exception {
        User user = new User("username", "r1", "r2");
        User fallback = randomBoolean() ? SystemUser.INSTANCE : null;
        when(firstRealm.token(threadContext)).thenReturn(token);
        when(firstRealm.supports(token)).thenReturn(true);
        mockAuthenticate(firstRealm, token, user);

        final AtomicBoolean completed = new AtomicBoolean(false);
        service.authenticate("_action", message, fallback, Version.CURRENT, ActionListener.wrap(result -> {
            assertThat(result, notNullValue());
            assertThat(result.getUser(), sameInstance(user));
            assertThreadContextContainsAuthentication(result);
            setCompletedToTrue(completed);
        }, this::logAndFail));

        verify(auditTrail).authenticationSuccess(firstRealm.name(), user, "_action", message);
        verifyNoMoreInteractions(auditTrail);
        assertTrue(completed.get());
    }

    public void testAuthenticateRestSuccess() throws Exception {
        User user1 = new User("username", "r1", "r2");
        when(firstRealm.token(threadContext)).thenReturn(token);
        when(firstRealm.supports(token)).thenReturn(true);
        mockAuthenticate(firstRealm, token, user1);
        // this call does not actually go async
        final AtomicBoolean completed = new AtomicBoolean(false);
        service.authenticate(restRequest, ActionListener.wrap(authentication -> {
            assertThat(authentication, notNullValue());
            assertThat(authentication.getUser(), sameInstance(user1));
            assertThreadContextContainsAuthentication(authentication);
            setCompletedToTrue(completed);
        }, this::logAndFail));
        verify(auditTrail).authenticationSuccess(firstRealm.name(), user1, restRequest);
        verifyNoMoreInteractions(auditTrail);
        assertTrue(completed.get());
    }

    public void testAutheticateTransportContextAndHeader() throws Exception {
        User user1 = new User("username", "r1", "r2");
        when(firstRealm.token(threadContext)).thenReturn(token);
        when(firstRealm.supports(token)).thenReturn(true);
        mockAuthenticate(firstRealm, token, user1);
        final AtomicBoolean completed = new AtomicBoolean(false);
        final SetOnce<Authentication> authRef = new SetOnce<>();
        final SetOnce<String> authHeaderRef = new SetOnce<>();
        try (ThreadContext.StoredContext ignore = threadContext.stashContext()) {
            service.authenticate("_action", message, SystemUser.INSTANCE, Version.CURRENT, ActionListener.wrap(authentication -> {

                assertThat(authentication, notNullValue());
                assertThat(authentication.getUser(), sameInstance(user1));
                assertThreadContextContainsAuthentication(authentication);
                authRef.set(authentication);
                authHeaderRef.set(threadContext.getHeader(Authentication.AUTHENTICATION_KEY));
                setCompletedToTrue(completed);
            }, this::logAndFail));
        }
        assertTrue(completed.compareAndSet(true, false));
        reset(firstRealm);

        // checking authentication from the context
        InternalMessage message1 = new InternalMessage();
        ThreadPool threadPool1 = new TestThreadPool("testAutheticateTransportContextAndHeader1");
        try {
            ThreadContext threadContext1 = threadPool1.getThreadContext();
            service = new AuthenticationService(Settings.EMPTY, realms, auditTrail, cryptoService,
                    new DefaultAuthenticationFailureHandler(), threadPool1, new AnonymousUser(Settings.EMPTY), tokenService);


            threadContext1.putTransient(Authentication.AUTHENTICATION_KEY, authRef.get());
            threadContext1.putHeader(Authentication.AUTHENTICATION_KEY, authHeaderRef.get());
            service.authenticate("_action", message1, SystemUser.INSTANCE, Version.CURRENT, ActionListener.wrap(ctxAuth -> {
                assertThat(ctxAuth, sameInstance(authRef.get()));
                assertThat(threadContext1.getHeader(Authentication.AUTHENTICATION_KEY), sameInstance(authHeaderRef.get()));
                setCompletedToTrue(completed);
            }, this::logAndFail));
            assertTrue(completed.compareAndSet(true, false));
            verifyZeroInteractions(firstRealm);
            reset(firstRealm);
        } finally {
            terminate(threadPool1);
        }

        // checking authentication from the user header
        ThreadPool threadPool2 = new TestThreadPool("testAutheticateTransportContextAndHeader2");
        try {
            ThreadContext threadContext2 = threadPool2.getThreadContext();
            final String header;
            try (ThreadContext.StoredContext ignore = threadContext2.stashContext()) {
                service = new AuthenticationService(Settings.EMPTY, realms, auditTrail, cryptoService,
                        new DefaultAuthenticationFailureHandler(), threadPool2, new AnonymousUser(Settings.EMPTY), tokenService);
                threadContext2.putHeader(Authentication.AUTHENTICATION_KEY, authHeaderRef.get());

                BytesStreamOutput output = new BytesStreamOutput();
                threadContext2.writeTo(output);
                StreamInput input = output.bytes().streamInput();
                threadContext2 = new ThreadContext(Settings.EMPTY);
                threadContext2.readHeaders(input);
                header = threadContext2.getHeader(Authentication.AUTHENTICATION_KEY);
            }

            threadPool2.getThreadContext().putHeader(Authentication.AUTHENTICATION_KEY, header);
            service = new AuthenticationService(Settings.EMPTY, realms, auditTrail, cryptoService,
                    new DefaultAuthenticationFailureHandler(), threadPool2, new AnonymousUser(Settings.EMPTY), tokenService);
            service.authenticate("_action", new InternalMessage(), SystemUser.INSTANCE, Version.CURRENT, ActionListener.wrap(result -> {
                assertThat(result, notNullValue());
                assertThat(result.getUser(), equalTo(user1));
                setCompletedToTrue(completed);
            }, this::logAndFail));
            assertTrue(completed.get());
            verifyZeroInteractions(firstRealm);
        } finally {
            terminate(threadPool2);
        }
    }

    public void testAuthenticateTamperedUser() throws Exception {
        InternalMessage message = new InternalMessage();
        threadContext.putHeader(Authentication.AUTHENTICATION_KEY, "_signed_auth");
        when(cryptoService.unsignAndVerify("_signed_auth", Version.CURRENT)).thenThrow(
                randomFrom(new RuntimeException(), new IllegalArgumentException(), new IllegalStateException()));

        try {
            authenticateBlocking("_action", message, randomBoolean() ? SystemUser.INSTANCE : null);
        } catch (Exception e) {
            //expected
            verify(auditTrail).tamperedRequest("_action", message);
            verifyNoMoreInteractions(auditTrail);
        }
    }

    public void testAttachIfMissing() throws Exception {
        User user;
        if (randomBoolean()) {
            user = SystemUser.INSTANCE;
        } else {
            user = new User("username", "r1", "r2");
        }
        assertThat(threadContext.getTransient(Authentication.AUTHENTICATION_KEY), nullValue());
        assertThat(threadContext.getHeader(Authentication.AUTHENTICATION_KEY), nullValue());
        service.attachUserIfMissing(user, Version.CURRENT);

        Authentication authentication = threadContext.getTransient(Authentication.AUTHENTICATION_KEY);
        assertThat(authentication, notNullValue());
        assertThat(authentication.getUser(), sameInstance((Object) user));
        assertThat(authentication.getLookedUpBy(), nullValue());
        assertThat(authentication.getAuthenticatedBy().getName(), is("__attach"));
        assertThat(authentication.getAuthenticatedBy().getType(), is("__attach"));
        assertThat(authentication.getAuthenticatedBy().getNodeName(), is("authc_test"));
        assertThreadContextContainsAuthentication(authentication);
    }

    public void testAttachIfMissingExists() throws Exception {
        Authentication authentication = new Authentication(new User("username", "r1", "r2"), new RealmRef("test", "test", "foo"), null);
        threadContext.putTransient(Authentication.AUTHENTICATION_KEY, authentication);
        threadContext.putHeader(Authentication.AUTHENTICATION_KEY, authentication.encode());
        service.attachUserIfMissing(new User("username2", "r3", "r4"), Version.CURRENT);
        assertThreadContextContainsAuthentication(authentication, false);
    }

    public void testAnonymousUserRest() throws Exception {
        String username = randomBoolean() ? AnonymousUser.DEFAULT_ANONYMOUS_USERNAME : "user1";
        Settings.Builder builder = Settings.builder()
                .putArray(AnonymousUser.ROLES_SETTING.getKey(), "r1", "r2", "r3")
                .put(XPackSettings.TRANSPORT_SSL_ENABLED.getKey(), useSsl);
        if (username.equals(AnonymousUser.DEFAULT_ANONYMOUS_USERNAME) == false) {
            builder.put(AnonymousUser.USERNAME_SETTING.getKey(), username);
        }
        Settings settings = builder.build();
        final AnonymousUser anonymousUser = new AnonymousUser(settings);
        service = new AuthenticationService(settings, realms, auditTrail, cryptoService, new DefaultAuthenticationFailureHandler(),
                threadPool, anonymousUser, tokenService);
        RestRequest request = new FakeRestRequest();

        Authentication result = authenticateBlocking(request);

        assertThat(result, notNullValue());
        assertThat(result.getUser(), sameInstance((Object) anonymousUser));
        assertThreadContextContainsAuthentication(result);
        verify(auditTrail).authenticationSuccess("__anonymous", new AnonymousUser(settings), request);
        verifyNoMoreInteractions(auditTrail);
    }

    public void testAnonymousUserTransportNoDefaultUser() throws Exception {
        Settings settings = Settings.builder()
                .putArray(AnonymousUser.ROLES_SETTING.getKey(), "r1", "r2", "r3")
                .put(XPackSettings.TRANSPORT_SSL_ENABLED.getKey(), useSsl)
                .build();
        final AnonymousUser anonymousUser = new AnonymousUser(settings);
        service = new AuthenticationService(settings, realms, auditTrail, cryptoService,
                new DefaultAuthenticationFailureHandler(), threadPool, anonymousUser, tokenService);
        InternalMessage message = new InternalMessage();

        Authentication result = authenticateBlocking("_action", message, null);
        assertThat(result, notNullValue());
        assertThat(result.getUser(), sameInstance(anonymousUser));
        assertThreadContextContainsAuthentication(result);
    }

    public void testAnonymousUserTransportWithDefaultUser() throws Exception {
        Settings settings = Settings.builder()
                .putArray(AnonymousUser.ROLES_SETTING.getKey(), "r1", "r2", "r3")
                .put(XPackSettings.TRANSPORT_SSL_ENABLED.getKey(), useSsl)
                .build();
        final AnonymousUser anonymousUser = new AnonymousUser(settings);
        service = new AuthenticationService(settings, realms, auditTrail, cryptoService,
                new DefaultAuthenticationFailureHandler(), threadPool, anonymousUser, tokenService);

        InternalMessage message = new InternalMessage();

        Authentication result = authenticateBlocking("_action", message, SystemUser.INSTANCE);
        assertThat(result, notNullValue());
        assertThat(result.getUser(), sameInstance(SystemUser.INSTANCE));
        assertThreadContextContainsAuthentication(result);
    }

    public void testRealmTokenThrowingException() throws Exception {
        when(firstRealm.token(threadContext)).thenThrow(authenticationError("realm doesn't like tokens"));
        try {
            authenticateBlocking("_action", message, null);
            fail("exception should bubble out");
        } catch (ElasticsearchException e) {
            assertThat(e.getMessage(), is("realm doesn't like tokens"));
            verify(auditTrail).authenticationFailed("_action", message);
        }
    }

    public void testRealmTokenThrowingExceptionRest() throws Exception {
        when(firstRealm.token(threadContext)).thenThrow(authenticationError("realm doesn't like tokens"));
        try {
            authenticateBlocking(restRequest);
            fail("exception should bubble out");
        } catch (ElasticsearchException e) {
            assertThat(e.getMessage(), is("realm doesn't like tokens"));
            verify(auditTrail).authenticationFailed(restRequest);
        }
    }

    public void testRealmSupportsMethodThrowingException() throws Exception {
        AuthenticationToken token = mock(AuthenticationToken.class);
        when(secondRealm.token(threadContext)).thenReturn(token);
        when(secondRealm.supports(token)).thenThrow(authenticationError("realm doesn't like supports"));
        try {
            authenticateBlocking("_action", message, null);
            fail("exception should bubble out");
        } catch (ElasticsearchException e) {
            assertThat(e.getMessage(), is("realm doesn't like supports"));
            verify(auditTrail).authenticationFailed(token, "_action", message);
        }
    }

    public void testRealmSupportsMethodThrowingExceptionRest() throws Exception {
        AuthenticationToken token = mock(AuthenticationToken.class);
        when(secondRealm.token(threadContext)).thenReturn(token);
        when(secondRealm.supports(token)).thenThrow(authenticationError("realm doesn't like supports"));
        try {
            authenticateBlocking(restRequest);
            fail("exception should bubble out");
        } catch (ElasticsearchException e) {
            assertThat(e.getMessage(), is("realm doesn't like supports"));
            verify(auditTrail).authenticationFailed(token, restRequest);
        }
    }

    public void testRealmAuthenticateThrowingException() throws Exception {
        AuthenticationToken token = mock(AuthenticationToken.class);
        when(secondRealm.token(threadContext)).thenReturn(token);
        when(secondRealm.supports(token)).thenReturn(true);
        doThrow(authenticationError("realm doesn't like authenticate"))
            .when(secondRealm).authenticate(eq(token), any(ActionListener.class));
        try {
            authenticateBlocking("_action", message, null);
            fail("exception should bubble out");
        } catch (ElasticsearchException e) {
            assertThat(e.getMessage(), is("realm doesn't like authenticate"));
            verify(auditTrail).authenticationFailed(token, "_action", message);
        }
    }

    public void testRealmAuthenticateThrowingExceptionRest() throws Exception {
        AuthenticationToken token = mock(AuthenticationToken.class);
        when(secondRealm.token(threadContext)).thenReturn(token);
        when(secondRealm.supports(token)).thenReturn(true);
        doThrow(authenticationError("realm doesn't like authenticate"))
                .when(secondRealm).authenticate(eq(token), any(ActionListener.class));
        try {
            authenticateBlocking(restRequest);
            fail("exception should bubble out");
        } catch (ElasticsearchSecurityException e) {
            assertThat(e.getMessage(), is("realm doesn't like authenticate"));
            verify(auditTrail).authenticationFailed(token, restRequest);
        }
    }

    public void testRealmLookupThrowingException() throws Exception {
        AuthenticationToken token = mock(AuthenticationToken.class);
        threadContext.putHeader(AuthenticationService.RUN_AS_USER_HEADER, "run_as");
        when(secondRealm.token(threadContext)).thenReturn(token);
        when(secondRealm.supports(token)).thenReturn(true);
        mockAuthenticate(secondRealm, token, new User("lookup user", new String[]{"user"}));
        doThrow(authenticationError("realm doesn't want to lookup"))
            .when(secondRealm).lookupUser(eq("run_as"), any(ActionListener.class));
        when(secondRealm.userLookupSupported()).thenReturn(true);

        try {
            authenticateBlocking("_action", message, null);
            fail("exception should bubble out");
        } catch (ElasticsearchException e) {
            assertThat(e.getMessage(), is("realm doesn't want to lookup"));
            verify(auditTrail).authenticationFailed(token, "_action", message);
        }
    }

    public void testRealmLookupThrowingExceptionRest() throws Exception {
        AuthenticationToken token = mock(AuthenticationToken.class);
        threadContext.putHeader(AuthenticationService.RUN_AS_USER_HEADER, "run_as");
        when(secondRealm.token(threadContext)).thenReturn(token);
        when(secondRealm.supports(token)).thenReturn(true);
        mockAuthenticate(secondRealm, token, new User("lookup user", new String[]{"user"}));
        doThrow(authenticationError("realm doesn't want to " + "lookup"))
                .when(secondRealm).lookupUser(eq("run_as"), any(ActionListener.class));
        when(secondRealm.userLookupSupported()).thenReturn(true);

        try {
            authenticateBlocking(restRequest);
            fail("exception should bubble out");
        } catch (ElasticsearchException e) {
            assertThat(e.getMessage(), is("realm doesn't want to lookup"));
            verify(auditTrail).authenticationFailed(token, restRequest);
        }
    }

    public void testRunAsLookupSameRealm() throws Exception {
        AuthenticationToken token = mock(AuthenticationToken.class);
        threadContext.putHeader(AuthenticationService.RUN_AS_USER_HEADER, "run_as");
        when(secondRealm.token(threadContext)).thenReturn(token);
        when(secondRealm.supports(token)).thenReturn(true);
        final User user = new User("lookup user", new String[]{"user"}, "lookup user", "lookup@foo.foo",
                Collections.singletonMap("foo", "bar"), true);
        mockAuthenticate(secondRealm, token, user);
        doAnswer((i) -> {
            ActionListener listener = (ActionListener) i.getArguments()[1];
            listener.onResponse(new User("looked up user", new String[]{"some role"}));
            return null;
        }).when(secondRealm).lookupUser(eq("run_as"), any(ActionListener.class));
        when(secondRealm.userLookupSupported()).thenReturn(true);

        final AtomicBoolean completed = new AtomicBoolean(false);
        ActionListener<Authentication> listener = ActionListener.wrap(result -> {
            assertThat(result, notNullValue());
            User authenticated = result.getUser();

            assertThat(SystemUser.is(authenticated), is(false));
            assertThat(authenticated.runAs(), is(notNullValue()));
            assertThat(authenticated.principal(), is("lookup user"));
            assertThat(authenticated.roles(), arrayContaining("user"));
            assertEquals(user.metadata(), authenticated.metadata());
            assertEquals(user.email(), authenticated.email());
            assertEquals(user.enabled(), authenticated.enabled());
            assertEquals(user.fullName(), authenticated.fullName());

            assertThat(authenticated.runAs().principal(), is("looked up user"));
            assertThat(authenticated.runAs().roles(), arrayContaining("some role"));
            assertThreadContextContainsAuthentication(result);
            setCompletedToTrue(completed);
        }, this::logAndFail);

        // we do not actually go async
        if (randomBoolean()) {
            service.authenticate("_action", message, null, Version.CURRENT, listener);
        } else {
            service.authenticate(restRequest, listener);
        }
        assertTrue(completed.get());
    }

    public void testRunAsLookupDifferentRealm() throws Exception {
        AuthenticationToken token = mock(AuthenticationToken.class);
        threadContext.putHeader(AuthenticationService.RUN_AS_USER_HEADER, "run_as");
        when(secondRealm.token(threadContext)).thenReturn(token);
        when(secondRealm.supports(token)).thenReturn(true);
        mockAuthenticate(secondRealm, token, new User("lookup user", new String[]{"user"}));
        when(firstRealm.userLookupSupported()).thenReturn(true);
        doAnswer((i) -> {
            ActionListener listener = (ActionListener) i.getArguments()[1];
            listener.onResponse(new User("looked up user", new String[]{"some role"}));
            return null;
        }).when(firstRealm).lookupUser(eq("run_as"), any(ActionListener.class));
        when(firstRealm.userLookupSupported()).thenReturn(true);

        final AtomicBoolean completed = new AtomicBoolean(false);
        ActionListener<Authentication> listener = ActionListener.wrap(result -> {
            assertThat(result, notNullValue());
            User authenticated = result.getUser();

            assertThat(SystemUser.is(authenticated), is(false));
            assertThat(authenticated.runAs(), is(notNullValue()));
            assertThat(authenticated.principal(), is("lookup user"));
            assertThat(authenticated.roles(), arrayContaining("user"));
            assertThat(authenticated.runAs().principal(), is("looked up user"));
            assertThat(authenticated.runAs().roles(), arrayContaining("some role"));
            assertThreadContextContainsAuthentication(result);
            setCompletedToTrue(completed);
        }, this::logAndFail);

        // call service asynchronously but it doesn't actually go async
        if (randomBoolean()) {
            service.authenticate("_action", message, null, Version.CURRENT, listener);
        } else {
            service.authenticate(restRequest, listener);
        }
        assertTrue(completed.get());
    }

    public void testRunAsWithEmptyRunAsUsernameRest() throws Exception {
        AuthenticationToken token = mock(AuthenticationToken.class);
        User user = new User("lookup user", new String[]{"user"});
        threadContext.putHeader(AuthenticationService.RUN_AS_USER_HEADER, "");
        when(secondRealm.token(threadContext)).thenReturn(token);
        when(secondRealm.supports(token)).thenReturn(true);
        mockAuthenticate(secondRealm, token, user);
        when(secondRealm.userLookupSupported()).thenReturn(true);

        try {
            authenticateBlocking(restRequest);
            fail("exception should be thrown");
        } catch (ElasticsearchException e) {
            verify(auditTrail).runAsDenied(any(User.class), eq(restRequest));
            verifyNoMoreInteractions(auditTrail);
        }
    }

    public void testRunAsWithEmptyRunAsUsername() throws Exception {
        AuthenticationToken token = mock(AuthenticationToken.class);
        User user = new User("lookup user", new String[]{"user"});
        threadContext.putHeader(AuthenticationService.RUN_AS_USER_HEADER, "");
        when(secondRealm.token(threadContext)).thenReturn(token);
        when(secondRealm.supports(token)).thenReturn(true);
        mockAuthenticate(secondRealm, token, user);
        when(secondRealm.userLookupSupported()).thenReturn(true);

        try {
            authenticateBlocking("_action", message, null);
            fail("exception should be thrown");
        } catch (ElasticsearchException e) {
            verify(auditTrail).runAsDenied(any(User.class), eq("_action"), eq(message));
            verifyNoMoreInteractions(auditTrail);
        }
    }

    public void testAuthenticateTransportDisabledRunAsUser() throws Exception {
        AuthenticationToken token = mock(AuthenticationToken.class);
        threadContext.putHeader(AuthenticationService.RUN_AS_USER_HEADER, "run_as");
        when(secondRealm.token(threadContext)).thenReturn(token);
        when(secondRealm.supports(token)).thenReturn(true);
        mockAuthenticate(secondRealm, token, new User("lookup user", new String[]{"user"}));
        doAnswer((i) -> {
            ActionListener listener = (ActionListener) i.getArguments()[1];
            listener.onResponse(new User("looked up user", new String[]{"some role"}, null, null, null, false));
            return null;
        }).when(secondRealm).lookupUser(eq("run_as"), any(ActionListener.class));
        when(secondRealm.userLookupSupported()).thenReturn(true);
        User fallback = randomBoolean() ? SystemUser.INSTANCE : null;
        ElasticsearchSecurityException e =
                expectThrows(ElasticsearchSecurityException.class, () -> authenticateBlocking("_action", message, fallback));
        verify(auditTrail).authenticationFailed(token, "_action", message);
        verifyNoMoreInteractions(auditTrail);
        assertAuthenticationException(e);
    }

    public void testAuthenticateRestDisabledRunAsUser() throws Exception {
        AuthenticationToken token = mock(AuthenticationToken.class);
        threadContext.putHeader(AuthenticationService.RUN_AS_USER_HEADER, "run_as");
        when(secondRealm.token(threadContext)).thenReturn(token);
        when(secondRealm.supports(token)).thenReturn(true);
        mockAuthenticate(secondRealm, token, new User("lookup user", new String[]{"user"}));
        doAnswer((i) -> {
            ActionListener listener = (ActionListener) i.getArguments()[1];
            listener.onResponse(new User("looked up user", new String[]{"some role"}, null, null, null, false));
            return null;
        }).when(secondRealm).lookupUser(eq("run_as"), any(ActionListener.class));
        when(secondRealm.userLookupSupported()).thenReturn(true);

        ElasticsearchSecurityException e =
                expectThrows(ElasticsearchSecurityException.class, () -> authenticateBlocking(restRequest));
        verify(auditTrail).authenticationFailed(token, restRequest);
        verifyNoMoreInteractions(auditTrail);
        assertAuthenticationException(e);
    }

    public void testAuthenticateWithToken() throws Exception {
        User user = new User("_username", "r1");
        final AtomicBoolean completed = new AtomicBoolean(false);
        final Authentication expected = new Authentication(user, new RealmRef("realm", "custom", "node"), null);
        String token = tokenService.getUserTokenString(tokenService.createUserToken(expected));
        try (ThreadContext.StoredContext ignore = threadContext.stashContext()) {
            threadContext.putHeader("Authorization", "Bearer " + token);
            service.authenticate("_action", message, null, Version.CURRENT, ActionListener.wrap(result -> {
                assertThat(result, notNullValue());
                assertThat(result.getUser(), is(user));
                assertThat(result.getLookedUpBy(), is(nullValue()));
                assertThat(result.getAuthenticatedBy(), is(notNullValue()));
                assertEquals(expected, result);
                setCompletedToTrue(completed);
            }, this::logAndFail));
        }
        assertTrue(completed.get());
        verify(auditTrail).authenticationSuccess("realm", user, "_action", message);
        verifyNoMoreInteractions(auditTrail);
    }

    public void testInvalidToken() throws Exception {
        final User user = new User("_username", "r1");
        when(firstRealm.token(threadContext)).thenReturn(token);
        when(firstRealm.supports(token)).thenReturn(true);
        mockAuthenticate(firstRealm, token, user);
        final int numBytes = randomIntBetween(TokenService.MINIMUM_BYTES, TokenService.MINIMUM_BYTES + 32);
        final byte[] randomBytes = new byte[numBytes];
        random().nextBytes(randomBytes);
        final CountDownLatch latch = new CountDownLatch(1);
        final Authentication expected = new Authentication(user, new RealmRef(firstRealm.name(), firstRealm.type(), "authc_test"), null);
        try (ThreadContext.StoredContext ignore = threadContext.stashContext()) {
            threadContext.putHeader("Authorization", "Bearer " + Base64.getEncoder().encodeToString(randomBytes));
            service.authenticate("_action", message, null, Version.CURRENT, ActionListener.wrap(result -> {
                assertThat(result, notNullValue());
                assertThat(result.getUser(), is(user));
                assertThat(result.getLookedUpBy(), is(nullValue()));
                assertThat(result.getAuthenticatedBy(), is(notNullValue()));
                assertThreadContextContainsAuthentication(result);
                assertEquals(expected, result);
                latch.countDown();
            }, this::logAndFail));
        }

        // we need to use a latch here because the key computation goes async on another thread!
        latch.await();
        verify(auditTrail).authenticationSuccess(firstRealm.name(), user, "_action", message);
        verifyNoMoreInteractions(auditTrail);
    }

    public void testExpiredToken() throws Exception {
        User user = new User("_username", "r1");
        final Authentication expected = new Authentication(user, new RealmRef("realm", "custom", "node"), null);
        String token = tokenService.getUserTokenString(tokenService.createUserToken(expected));
        when(lifecycleService.isSecurityIndexAvailable()).thenReturn(true);
        doAnswer(invocationOnMock -> {
            ActionListener<GetResponse> listener = (ActionListener<GetResponse>) invocationOnMock.getArguments()[2];
            GetResponse response = mock(GetResponse.class);
            when(response.isExists()).thenReturn(true);
            listener.onResponse(response);
            return Void.TYPE;
        }).when(client).execute(eq(GetAction.INSTANCE), any(GetRequest.class), any(ActionListener.class));

        try (ThreadContext.StoredContext ignore = threadContext.stashContext()) {
            threadContext.putHeader("Authorization", "Bearer " + token);
            ElasticsearchSecurityException e =
                    expectThrows(ElasticsearchSecurityException.class, () -> authenticateBlocking("_action", message, null));
            assertEquals(RestStatus.UNAUTHORIZED, e.status());
            assertEquals("token expired", e.getMessage());
        }
    }

    private static class InternalMessage extends TransportMessage {
    }

    void assertThreadContextContainsAuthentication(Authentication authentication) throws IOException {
        assertThreadContextContainsAuthentication(authentication, this.useSsl == false);
    }

    private void assertThreadContextContainsAuthentication(Authentication authentication, boolean expectSignedHeaders) throws IOException {
        Authentication contextAuth = threadContext.getTransient(Authentication.AUTHENTICATION_KEY);
        assertThat(contextAuth, notNullValue());
        assertThat(contextAuth, is(authentication));
        final Object expectedHeader ;
        if (expectSignedHeaders) {
            expectedHeader = SIGNING_PREFIX + authentication.encode();
        } else {
            expectedHeader = authentication.encode();
        }
        assertThat(threadContext.getHeader(Authentication.AUTHENTICATION_KEY), equalTo(expectedHeader));
    }

    private void mockAuthenticate(Realm realm, AuthenticationToken token, User user) {
        doAnswer((i) -> {
            ActionListener listener = (ActionListener) i.getArguments()[1];
            listener.onResponse(user);
            return null;
        }).when(realm).authenticate(eq(token), any(ActionListener.class));
    }

    private Authentication authenticateBlocking(RestRequest restRequest) {
        PlainActionFuture<Authentication> future = new PlainActionFuture<>();
        service.authenticate(restRequest, future);
        return future.actionGet();
    }

    private Authentication authenticateBlocking(String action, TransportMessage message, User fallbackUser) {
        PlainActionFuture<Authentication> future = new PlainActionFuture<>();
        service.authenticate(action, message, fallbackUser, Version.CURRENT, future);
        return future.actionGet();
    }

    static class TestRealms extends Realms {

        TestRealms(Settings settings, Environment env, Map<String, Factory> factories, XPackLicenseState licenseState,
                   ThreadContext threadContext, ReservedRealm reservedRealm, List<Realm> realms, List<Realm> internalRealms)
                throws Exception {
            super(settings, env, factories, licenseState, threadContext, reservedRealm);
            this.realms = realms;
            this.internalRealmsOnly = internalRealms;
        }
    }

    private void logAndFail(Exception e) {
        logger.error("unexpected exception", e);
        fail("unexpected exception " + e.getMessage());
    }

    private void setCompletedToTrue(AtomicBoolean completed) {
        assertTrue(completed.compareAndSet(false, true));
    }
}
