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

package org.elasticsearch.xpack.security.transport;

import org.elasticsearch.Version;
import org.elasticsearch.action.support.DestructiveOperations;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.env.Environment;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.test.VersionUtils;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.Transport;
import org.elasticsearch.transport.Transport.Connection;
import org.elasticsearch.transport.TransportException;
import org.elasticsearch.transport.TransportInterceptor.AsyncSender;
import org.elasticsearch.transport.TransportRequest;
import org.elasticsearch.transport.TransportRequestOptions;
import org.elasticsearch.transport.TransportResponse;
import org.elasticsearch.transport.TransportResponse.Empty;
import org.elasticsearch.transport.TransportResponseHandler;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.XPackSettings;
import org.elasticsearch.xpack.security.SecurityContext;
import org.elasticsearch.xpack.security.authc.Authentication;
import org.elasticsearch.xpack.security.authc.Authentication.RealmRef;
import org.elasticsearch.xpack.security.authc.AuthenticationService;
import org.elasticsearch.xpack.security.authz.AuthorizationService;
import org.elasticsearch.xpack.security.crypto.CryptoService;
import org.elasticsearch.xpack.security.user.KibanaUser;
import org.elasticsearch.xpack.security.user.SystemUser;
import org.elasticsearch.xpack.security.user.User;
import org.elasticsearch.xpack.ssl.SSLService;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class SecurityServerTransportInterceptorTests extends ESTestCase {

    private Settings settings;
    private ThreadPool threadPool;
    private ThreadContext threadContext;
    private XPackLicenseState xPackLicenseState;
    private CryptoService cryptoService;
    private SecurityContext securityContext;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        settings = Settings.builder()
                .put("path.home", createTempDir())
                .put(XPackSettings.TRANSPORT_SSL_ENABLED.getKey(), true)
                .build();
        threadPool = mock(ThreadPool.class);
        threadContext = new ThreadContext(settings);
        when(threadPool.getThreadContext()).thenReturn(threadContext);
        cryptoService = new CryptoService(settings, new Environment(settings));
        securityContext = spy(new SecurityContext(settings, threadPool.getThreadContext(), cryptoService));
        xPackLicenseState = mock(XPackLicenseState.class);
        when(xPackLicenseState.isAuthAllowed()).thenReturn(true);
    }

    public void testSendAsyncUnlicensed() {
        SecurityServerTransportInterceptor interceptor = new SecurityServerTransportInterceptor(settings, threadPool,
                mock(AuthenticationService.class), mock(AuthorizationService.class), xPackLicenseState, mock(SSLService.class),
                securityContext, new DestructiveOperations(Settings.EMPTY, new ClusterSettings(Settings.EMPTY,
                Collections.singleton(DestructiveOperations.REQUIRES_NAME_SETTING))));
        when(xPackLicenseState.isAuthAllowed()).thenReturn(false);
        AtomicBoolean calledWrappedSender = new AtomicBoolean(false);
        AsyncSender sender = interceptor.interceptSender(new AsyncSender() {
            @Override
            public <T extends TransportResponse> void sendRequest(Transport.Connection connection, String action, TransportRequest request,
                                                                  TransportRequestOptions options, TransportResponseHandler<T> handler) {
                if (calledWrappedSender.compareAndSet(false, true) == false) {
                    fail("sender called more than once!");
                }
            }
        });
        sender.sendRequest(null, null, null, null, null);
        assertTrue(calledWrappedSender.get());
        verify(xPackLicenseState).isAuthAllowed();
        verifyNoMoreInteractions(xPackLicenseState);
        verifyZeroInteractions(securityContext);
    }

    public void testSendAsync() throws Exception {
        final User authUser = randomBoolean() ? new User("authenticator") : null;
        final User user = new User("test", randomRoles(), authUser);
        final Authentication authentication = new Authentication(user, new RealmRef("ldap", "foo", "node1"), null);
        authentication.writeToContext(threadContext, cryptoService, settings, Version.CURRENT, true);
        SecurityServerTransportInterceptor interceptor = new SecurityServerTransportInterceptor(settings, threadPool,
                mock(AuthenticationService.class), mock(AuthorizationService.class), xPackLicenseState, mock(SSLService.class),
                securityContext, new DestructiveOperations(Settings.EMPTY, new ClusterSettings(Settings.EMPTY,
                Collections.singleton(DestructiveOperations.REQUIRES_NAME_SETTING))));

        AtomicBoolean calledWrappedSender = new AtomicBoolean(false);
        AtomicReference<User> sendingUser = new AtomicReference<>();
        AsyncSender sender = interceptor.interceptSender(new AsyncSender() {
            @Override
            public <T extends TransportResponse> void sendRequest(Transport.Connection connection, String action, TransportRequest request,
                                                                  TransportRequestOptions options, TransportResponseHandler<T> handler) {
                if (calledWrappedSender.compareAndSet(false, true) == false) {
                    fail("sender called more than once!");
                }
                sendingUser.set(securityContext.getUser());
            }
        });
        Transport.Connection connection = mock(Transport.Connection.class);
        when(connection.getVersion()).thenReturn(Version.CURRENT);
        sender.sendRequest(connection, "indices:foo", null, null, null);
        assertTrue(calledWrappedSender.get());
        assertEquals(user, sendingUser.get());
        assertEquals(user, securityContext.getUser());
        verify(xPackLicenseState).isAuthAllowed();
        verify(securityContext, never()).executeAsUser(any(User.class), any(Consumer.class), any(Version.class));
        verifyNoMoreInteractions(xPackLicenseState);
    }

    public void testSendAsyncSwitchToSystem() throws Exception {
        final User authUser = randomBoolean() ? new User("authenticator") : null;
        final User user = new User("test", randomRoles(), authUser);
        final Authentication authentication = new Authentication(user, new RealmRef("ldap", "foo", "node1"), null);
        authentication.writeToContext(threadContext, cryptoService, settings, Version.CURRENT, true);
        threadContext.putTransient(AuthorizationService.ORIGINATING_ACTION_KEY, "indices:foo");

        SecurityServerTransportInterceptor interceptor = new SecurityServerTransportInterceptor(settings, threadPool,
                mock(AuthenticationService.class), mock(AuthorizationService.class), xPackLicenseState, mock(SSLService.class),
                securityContext, new DestructiveOperations(Settings.EMPTY, new ClusterSettings(Settings.EMPTY,
                Collections.singleton(DestructiveOperations.REQUIRES_NAME_SETTING))));

        AtomicBoolean calledWrappedSender = new AtomicBoolean(false);
        AtomicReference<User> sendingUser = new AtomicReference<>();
        AsyncSender sender = interceptor.interceptSender(new AsyncSender() {
            @Override
            public <T extends TransportResponse> void sendRequest(Transport.Connection connection, String action, TransportRequest request,
                                                                  TransportRequestOptions options, TransportResponseHandler<T> handler) {
                if (calledWrappedSender.compareAndSet(false, true) == false) {
                    fail("sender called more than once!");
                }
                sendingUser.set(securityContext.getUser());
            }
        });
        Connection connection = mock(Connection.class);
        when(connection.getVersion()).thenReturn(Version.CURRENT);
        sender.sendRequest(connection, "internal:foo", null, null, null);
        assertTrue(calledWrappedSender.get());
        assertNotEquals(user, sendingUser.get());
        assertEquals(SystemUser.INSTANCE, sendingUser.get());
        assertEquals(user, securityContext.getUser());
        verify(xPackLicenseState).isAuthAllowed();
        verify(securityContext).executeAsUser(any(User.class), any(Consumer.class), any(Version.class));
        verifyNoMoreInteractions(xPackLicenseState);
    }

    public void testSendAsyncToNodeThatRequiresSigning() throws Exception {
        final User user = new User("test");
        final Authentication authentication = new Authentication(user, new RealmRef("ldap", "foo", "node1"), null);
        // sanity check signing is off
        assertFalse(Authentication.shouldSign(settings, Version.CURRENT, true));
        authentication.writeToContext(threadContext, cryptoService, settings, Version.CURRENT, true);
        threadContext.putTransient(AuthorizationService.ORIGINATING_ACTION_KEY, "indices:foo");

        SecurityServerTransportInterceptor interceptor = new SecurityServerTransportInterceptor(settings, threadPool,
                mock(AuthenticationService.class), mock(AuthorizationService.class), xPackLicenseState, mock(SSLService.class),
                securityContext, new DestructiveOperations(Settings.EMPTY, new ClusterSettings(Settings.EMPTY,
                Collections.singleton(DestructiveOperations.REQUIRES_NAME_SETTING))));

        AtomicBoolean calledWrappedSender = new AtomicBoolean(false);
        AtomicReference<User> sendingUser = new AtomicReference<>();
        AsyncSender sender = interceptor.interceptSender(new AsyncSender() {
            @Override
            public <T extends TransportResponse> void sendRequest(Transport.Connection connection, String action, TransportRequest request,
                                                                  TransportRequestOptions options, TransportResponseHandler<T> handler) {
                if (calledWrappedSender.compareAndSet(false, true) == false) {
                    fail("sender called more than once!");
                }
                sendingUser.set(securityContext.getUser());
            }
        });
        Connection connection = mock(Connection.class);
        final Version remoteVersion = Version.fromId(randomIntBetween(Version.V_5_0_0_ID, Version.V_5_4_0_ID - 100));
        when(connection.getVersion()).thenReturn(remoteVersion);
        // sanity check that remote node requires signing
        assertTrue(Authentication.shouldSign(settings, remoteVersion, true));
        sender.sendRequest(connection, "indices:foo", null, null, null);
        assertTrue(calledWrappedSender.get());
        assertEquals(user, sendingUser.get());
        assertEquals(user, securityContext.getUser());
        verify(xPackLicenseState).isAuthAllowed();
        verify(securityContext).executeAfterRewritingAuthentication(any(Consumer.class), eq(remoteVersion));
        verifyNoMoreInteractions(xPackLicenseState);
    }

    public void testSendWithoutUser() throws Exception {
        SecurityServerTransportInterceptor interceptor = new SecurityServerTransportInterceptor(settings, threadPool,
                mock(AuthenticationService.class), mock(AuthorizationService.class), xPackLicenseState, mock(SSLService.class),
                securityContext, new DestructiveOperations(Settings.EMPTY, new ClusterSettings(Settings.EMPTY,
                Collections.singleton(DestructiveOperations.REQUIRES_NAME_SETTING))));

        assertNull(securityContext.getUser());
        AsyncSender sender = interceptor.interceptSender(new AsyncSender() {
            @Override
            public <T extends TransportResponse> void sendRequest(Transport.Connection connection, String action, TransportRequest request,
                                                                  TransportRequestOptions options, TransportResponseHandler<T> handler) {
                fail("sender should not be called!");
            }
        });
        Transport.Connection connection = mock(Transport.Connection.class);
        when(connection.getVersion()).thenReturn(Version.CURRENT);
        IllegalStateException e =
                expectThrows(IllegalStateException.class, () -> sender.sendRequest(connection, "indices:foo", null, null, null));
        assertEquals("there should always be a user when sending a message", e.getMessage());
        assertNull(securityContext.getUser());
        verify(xPackLicenseState).isAuthAllowed();
        verify(securityContext, never()).executeAsUser(any(User.class), any(Consumer.class), any(Version.class));
        verifyNoMoreInteractions(xPackLicenseState);
    }

    public void testSendWithKibanaUser() throws Exception {
        final User user = new KibanaUser(true);
        final Authentication authentication = new Authentication(user, new RealmRef("reserved", "reserved", "node1"), null);
        authentication.writeToContext(threadContext, cryptoService, settings, Version.CURRENT, true);
        threadContext.putTransient(AuthorizationService.ORIGINATING_ACTION_KEY, "indices:foo");

        SecurityServerTransportInterceptor interceptor = new SecurityServerTransportInterceptor(settings, threadPool,
                mock(AuthenticationService.class), mock(AuthorizationService.class), xPackLicenseState, mock(SSLService.class),
                securityContext, new DestructiveOperations(Settings.EMPTY, new ClusterSettings(Settings.EMPTY,
                Collections.singleton(DestructiveOperations.REQUIRES_NAME_SETTING))));

        AtomicBoolean calledWrappedSender = new AtomicBoolean(false);
        AtomicReference<User> sendingUser = new AtomicReference<>();
        AsyncSender intercepted = new AsyncSender() {
            @Override
            public <T extends TransportResponse> void sendRequest(Transport.Connection connection, String action, TransportRequest request,
                                                                  TransportRequestOptions options, TransportResponseHandler<T> handler) {
                if (calledWrappedSender.compareAndSet(false, true) == false) {
                    fail("sender called more than once!");
                }
                sendingUser.set(securityContext.getUser());
            }
        };
        AsyncSender sender = interceptor.interceptSender(intercepted);
        Transport.Connection connection = mock(Transport.Connection.class);
        when(connection.getVersion()).thenReturn(Version.fromId(randomIntBetween(Version.V_5_0_0_ID, Version.V_5_2_0_ID - 100)));
        sender.sendRequest(connection, "indices:foo[s]", null, null, null);
        assertTrue(calledWrappedSender.get());
        assertNotEquals(user, sendingUser.get());
        assertEquals(KibanaUser.NAME, sendingUser.get().principal());
        assertThat(sendingUser.get().roles(), arrayContaining("kibana"));
        assertEquals(user, securityContext.getUser());

        // reset and test with version that was changed
        calledWrappedSender.set(false);
        sendingUser.set(null);
        when(connection.getVersion()).thenReturn(Version.V_5_2_0);
        sender.sendRequest(connection, "indices:foo[s]", null, null, null);
        assertTrue(calledWrappedSender.get());
        assertEquals(user, sendingUser.get());

        // reset and disable reserved realm
        calledWrappedSender.set(false);
        sendingUser.set(null);
        when(connection.getVersion()).thenReturn(Version.V_5_0_0);
        settings = Settings.builder().put(settings).put(XPackSettings.RESERVED_REALM_ENABLED_SETTING.getKey(), false).build();
        interceptor = new SecurityServerTransportInterceptor(settings, threadPool,
                mock(AuthenticationService.class), mock(AuthorizationService.class), xPackLicenseState, mock(SSLService.class),
                securityContext, new DestructiveOperations(Settings.EMPTY, new ClusterSettings(Settings.EMPTY,
                Collections.singleton(DestructiveOperations.REQUIRES_NAME_SETTING))));
        sender = interceptor.interceptSender(intercepted);
        sender.sendRequest(connection, "indices:foo[s]", null, null, null);
        assertTrue(calledWrappedSender.get());
        assertEquals(user, sendingUser.get());

        verify(xPackLicenseState, times(3)).isAuthAllowed();
        verify(securityContext, times(1)).executeAsUser(any(User.class), any(Consumer.class), any(Version.class));
        verifyNoMoreInteractions(xPackLicenseState);
    }

    public void testSendToNewerVersionSetsCorrectVersion() throws Exception {
        final User authUser = randomBoolean() ? new User("authenticator") : null;
        final User user = new User("joe", randomRoles(), authUser);
        final Authentication authentication = new Authentication(user, new RealmRef("file", "file", "node1"), null);
        authentication.writeToContext(threadContext, cryptoService, settings, Version.CURRENT, true);
        threadContext.putTransient(AuthorizationService.ORIGINATING_ACTION_KEY, "indices:foo");

        SecurityServerTransportInterceptor interceptor = new SecurityServerTransportInterceptor(settings, threadPool,
                mock(AuthenticationService.class), mock(AuthorizationService.class), xPackLicenseState, mock(SSLService.class),
                securityContext, new DestructiveOperations(Settings.EMPTY, new ClusterSettings(Settings.EMPTY,
                Collections.singleton(DestructiveOperations.REQUIRES_NAME_SETTING))));

        AtomicBoolean calledWrappedSender = new AtomicBoolean(false);
        AtomicReference<User> sendingUser = new AtomicReference<>();
        AtomicReference<Authentication> authRef = new AtomicReference<>();
        AsyncSender intercepted = new AsyncSender() {
            @Override
            public <T extends TransportResponse> void sendRequest(Transport.Connection connection, String action, TransportRequest request,
                                                                  TransportRequestOptions options, TransportResponseHandler<T> handler) {
                if (calledWrappedSender.compareAndSet(false, true) == false) {
                    fail("sender called more than once!");
                }
                sendingUser.set(securityContext.getUser());
                authRef.set(securityContext.getAuthentication());
            }
        };
        AsyncSender sender = interceptor.interceptSender(intercepted);
        final Version connectionVersion = Version.fromId(Version.CURRENT.id + randomIntBetween(100, 100000));
        assertEquals(Version.CURRENT, Version.min(connectionVersion, Version.CURRENT));

        Transport.Connection connection = mock(Transport.Connection.class);
        when(connection.getVersion()).thenReturn(connectionVersion);
        sender.sendRequest(connection, "indices:foo[s]", null, null, null);
        assertTrue(calledWrappedSender.get());
        assertEquals(user, sendingUser.get());
        assertEquals(user, securityContext.getUser());
        assertEquals(Version.CURRENT, authRef.get().getVersion());
        assertEquals(Version.CURRENT, authentication.getVersion());
    }

    public void testSendToOlderVersionSetsCorrectVersion() throws Exception {
        final User authUser = randomBoolean() ? new User("authenticator") : null;
        final User user = new User("joe", randomRoles(), authUser);
        final Authentication authentication = new Authentication(user, new RealmRef("file", "file", "node1"), null);
        authentication.writeToContext(threadContext, cryptoService, settings, Version.CURRENT, true);
        threadContext.putTransient(AuthorizationService.ORIGINATING_ACTION_KEY, "indices:foo");

        SecurityServerTransportInterceptor interceptor = new SecurityServerTransportInterceptor(settings, threadPool,
                mock(AuthenticationService.class), mock(AuthorizationService.class), xPackLicenseState, mock(SSLService.class),
                securityContext, new DestructiveOperations(Settings.EMPTY, new ClusterSettings(Settings.EMPTY,
                Collections.singleton(DestructiveOperations.REQUIRES_NAME_SETTING))));

        AtomicBoolean calledWrappedSender = new AtomicBoolean(false);
        AtomicReference<User> sendingUser = new AtomicReference<>();
        AtomicReference<Authentication> authRef = new AtomicReference<>();
        AsyncSender intercepted = new AsyncSender() {
            @Override
            public <T extends TransportResponse> void sendRequest(Transport.Connection connection, String action, TransportRequest request,
                                                                  TransportRequestOptions options, TransportResponseHandler<T> handler) {
                if (calledWrappedSender.compareAndSet(false, true) == false) {
                    fail("sender called more than once!");
                }
                sendingUser.set(securityContext.getUser());
                authRef.set(securityContext.getAuthentication());
            }
        };
        AsyncSender sender = interceptor.interceptSender(intercepted);
        final Version connectionVersion = Version.fromId(Version.CURRENT.id - randomIntBetween(100, 100000));
        assertEquals(connectionVersion, Version.min(connectionVersion, Version.CURRENT));

        Transport.Connection connection = mock(Transport.Connection.class);
        when(connection.getVersion()).thenReturn(connectionVersion);
        sender.sendRequest(connection, "indices:foo[s]", null, null, null);
        assertTrue(calledWrappedSender.get());
        assertEquals(user, sendingUser.get());
        assertEquals(user, securityContext.getUser());
        assertEquals(connectionVersion, authRef.get().getVersion());
        assertEquals(Version.CURRENT, authentication.getVersion());
    }

    // see #1576 for details
    public void test540HeaderBug() throws Exception {
        final User user = new User("joe", "role");
        final Authentication authentication = new Authentication(user, new RealmRef("file", "file", "node1"), null, Version.V_5_4_0);
        authentication.writeToContext(threadContext, cryptoService, settings, Version.V_5_4_0, true);
        threadContext.putTransient(AuthorizationService.ORIGINATING_ACTION_KEY, "indices:foo");
        threadContext.putTransient("BOOM", "Stash context otherwise 5.4.0 will be broken");
        SecurityServerTransportInterceptor interceptor = new SecurityServerTransportInterceptor(settings, threadPool,
                mock(AuthenticationService.class), mock(AuthorizationService.class), xPackLicenseState, mock(SSLService.class),
                securityContext, new DestructiveOperations(Settings.EMPTY, new ClusterSettings(Settings.EMPTY,
                Collections.singleton(DestructiveOperations.REQUIRES_NAME_SETTING))));

        AtomicBoolean calledWrappedSender = new AtomicBoolean(false);
        AtomicReference<User> sendingUser = new AtomicReference<>();
        AtomicReference<Authentication> authRef = new AtomicReference<>();
        AsyncSender intercepted = new AsyncSender() {
            @Override
            public <T extends TransportResponse> void sendRequest(Transport.Connection connection, String action, TransportRequest request,
                                                                  TransportRequestOptions options, TransportResponseHandler<T> handler) {
                if (calledWrappedSender.compareAndSet(false, true) == false) {
                    fail("sender called more than once!");
                }
                assertNull("context should have been rewritten", threadContext.getTransient("BOOM"));
                sendingUser.set(securityContext.getUser());
                authRef.set(securityContext.getAuthentication());
            }
        };
        AsyncSender sender = interceptor.interceptSender(intercepted);

        Transport.Connection connection = mock(Transport.Connection.class);
        when(connection.getVersion()).thenReturn(Version.V_5_4_0);
        sender.sendRequest(connection, "indices:foo[s]", null, null, null);
        assertTrue(calledWrappedSender.get());
        assertEquals(user, sendingUser.get());
        assertEquals(user, securityContext.getUser());
        assertEquals(Version.V_5_4_0, authRef.get().getVersion());
        assertEquals(Version.V_5_4_0, authentication.getVersion());
    }

    public void testContextRestoreResponseHandler() throws Exception {
        ThreadContext threadContext = new ThreadContext(Settings.EMPTY);

        threadContext.putTransient("foo", "bar");
        threadContext.putHeader("key", "value");
        try (ThreadContext.StoredContext storedContext = threadContext.stashContext()) {
            threadContext.putTransient("foo", "different_bar");
            threadContext.putHeader("key", "value2");
            TransportResponseHandler<Empty> handler = new TransportService.ContextRestoreResponseHandler<>(
                    threadContext.wrapRestorable(storedContext), new TransportResponseHandler<Empty>() {

                @Override
                public Empty newInstance() {
                    return Empty.INSTANCE;
                }

                @Override
                public void handleResponse(Empty response) {
                    assertEquals("bar", threadContext.getTransient("foo"));
                    assertEquals("value", threadContext.getHeader("key"));
                }

                @Override
                public void handleException(TransportException exp) {
                    assertEquals("bar", threadContext.getTransient("foo"));
                    assertEquals("value", threadContext.getHeader("key"));
                }

                @Override
                public String executor() {
                    return null;
                }
            });

            handler.handleResponse(null);
            handler.handleException(null);
        }
    }

    public void testContextRestoreResponseHandlerRestoreOriginalContext() throws Exception {
        try (ThreadContext threadContext = new ThreadContext(Settings.EMPTY)) {
            threadContext.putTransient("foo", "bar");
            threadContext.putHeader("key", "value");
            TransportResponseHandler<Empty> handler;
            try (ThreadContext.StoredContext ignore = threadContext.stashContext()) {
                threadContext.putTransient("foo", "different_bar");
                threadContext.putHeader("key", "value2");
                handler = new TransportService.ContextRestoreResponseHandler<>(threadContext.newRestorableContext(true),
                        new TransportResponseHandler<Empty>() {

                            @Override
                            public Empty newInstance() {
                                return Empty.INSTANCE;
                            }

                            @Override
                            public void handleResponse(Empty response) {
                                assertEquals("different_bar", threadContext.getTransient("foo"));
                                assertEquals("value2", threadContext.getHeader("key"));
                            }

                            @Override
                            public void handleException(TransportException exp) {
                                assertEquals("different_bar", threadContext.getTransient("foo"));
                                assertEquals("value2", threadContext.getHeader("key"));
                            }

                            @Override
                            public String executor() {
                                return null;
                            }
                        });
            }

            assertEquals("bar", threadContext.getTransient("foo"));
            assertEquals("value", threadContext.getHeader("key"));
            handler.handleResponse(null);

            assertEquals("bar", threadContext.getTransient("foo"));
            assertEquals("value", threadContext.getHeader("key"));
            handler.handleException(null);

            assertEquals("bar", threadContext.getTransient("foo"));
            assertEquals("value", threadContext.getHeader("key"));
        }
    }

    private String[] randomRoles() {
        return generateRandomStringArray(3, 10, false, true);
    }


}
