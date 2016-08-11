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

package org.elasticsearch.xpack.security.transport.netty4;

import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.ssl.SslHandler;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.network.NetworkService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.BigArrays;
import org.elasticsearch.env.Environment;
import org.elasticsearch.indices.breaker.CircuitBreakerService;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.netty4.Netty4MockUtil;
import org.elasticsearch.xpack.security.ssl.SSLService;
import org.elasticsearch.xpack.security.transport.SSLClientAuth;
import org.junit.Before;

import java.nio.file.Path;
import java.util.Locale;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;

public class SecurityNetty4TransportTests extends ESTestCase {

    private Environment env;
    private SSLService sslService;

    @Before
    public void createSSLService() throws Exception {
        Path testnodeStore = getDataPath("/org/elasticsearch/xpack/security/transport/ssl/certs/simple/testnode.jks");
        Settings settings = Settings.builder()
                .put("xpack.security.ssl.keystore.path", testnodeStore)
                .put("xpack.security.ssl.keystore.password", "testnode")
                .put("path.home", createTempDir())
                .build();
        env = new Environment(settings);
        sslService = new SSLService(settings, env);
    }

    private SecurityNetty4Transport createTransport(boolean sslEnabled) {
        return createTransport(sslEnabled, Settings.EMPTY);
    }

    private SecurityNetty4Transport createTransport(boolean sslEnabled, Settings additionalSettings) {
        final Settings settings =
                Settings.builder()
                        .put(SecurityNetty4Transport.SSL_SETTING.getKey(), sslEnabled)
                        .put(additionalSettings)
                        .build();
        return new SecurityNetty4Transport(
                settings,
                mock(ThreadPool.class),
                mock(NetworkService.class),
                mock(BigArrays.class),
                mock(NamedWriteableRegistry.class),
                mock(CircuitBreakerService.class),
                null,
                sslService);
    }

    public void testThatSSLCanBeDisabledByProfile() throws Exception {
        SecurityNetty4Transport transport = createTransport(true);
        Netty4MockUtil.setOpenChannelsHandlerToMock(transport);
        ChannelHandler handler = transport.getServerChannelInitializer("client",
                Settings.builder().put("xpack.security.ssl", false).build());
        final EmbeddedChannel ch = new EmbeddedChannel(handler);
        assertThat(ch.pipeline().get(SslHandler.class), nullValue());
    }

    public void testThatSSLCanBeEnabledByProfile() throws Exception {
        SecurityNetty4Transport transport = createTransport(false);
        Netty4MockUtil.setOpenChannelsHandlerToMock(transport);
        ChannelHandler handler = transport.getServerChannelInitializer("client",
                Settings.builder().put("xpack.security.ssl", true).build());
        final EmbeddedChannel ch = new EmbeddedChannel(handler);
        assertThat(ch.pipeline().get(SslHandler.class), notNullValue());
    }

    public void testThatProfileTakesDefaultSSLSetting() throws Exception {
        SecurityNetty4Transport transport = createTransport(true);
        Netty4MockUtil.setOpenChannelsHandlerToMock(transport);
        ChannelHandler handler = transport.getServerChannelInitializer("client", Settings.EMPTY);
        final EmbeddedChannel ch = new EmbeddedChannel(handler);
        assertThat(ch.pipeline().get(SslHandler.class).engine(), notNullValue());
    }

    public void testDefaultClientAuth() throws Exception {
        SecurityNetty4Transport transport = createTransport(true);
        Netty4MockUtil.setOpenChannelsHandlerToMock(transport);
        ChannelHandler handler = transport.getServerChannelInitializer("client", Settings.EMPTY);
        final EmbeddedChannel ch = new EmbeddedChannel(handler);
        assertThat(ch.pipeline().get(SslHandler.class).engine().getNeedClientAuth(), is(true));
        assertThat(ch.pipeline().get(SslHandler.class).engine().getWantClientAuth(), is(false));
    }

    public void testRequiredClientAuth() throws Exception {
        String value = randomFrom(SSLClientAuth.REQUIRED.name(), SSLClientAuth.REQUIRED.name().toLowerCase(Locale.ROOT), "true");
        SecurityNetty4Transport transport =
                createTransport(true, Settings.builder().put(SecurityNetty4Transport.CLIENT_AUTH_SETTING.getKey(), value).build());
        Netty4MockUtil.setOpenChannelsHandlerToMock(transport);
        ChannelHandler handler = transport.getServerChannelInitializer("client", Settings.EMPTY);
        final EmbeddedChannel ch = new EmbeddedChannel(handler);
        assertThat(ch.pipeline().get(SslHandler.class).engine().getNeedClientAuth(), is(true));
        assertThat(ch.pipeline().get(SslHandler.class).engine().getWantClientAuth(), is(false));
    }

    public void testNoClientAuth() throws Exception {
        String value = randomFrom(SSLClientAuth.NO.name(), "false", "FALSE", SSLClientAuth.NO.name().toLowerCase(Locale.ROOT));
        SecurityNetty4Transport transport =
                createTransport(true, Settings.builder().put(SecurityNetty4Transport.CLIENT_AUTH_SETTING.getKey(), value).build());
        Netty4MockUtil.setOpenChannelsHandlerToMock(transport);
        ChannelHandler handler = transport.getServerChannelInitializer("client", Settings.EMPTY);
        final EmbeddedChannel ch = new EmbeddedChannel(handler);
        assertThat(ch.pipeline().get(SslHandler.class).engine().getNeedClientAuth(), is(false));
        assertThat(ch.pipeline().get(SslHandler.class).engine().getWantClientAuth(), is(false));
    }

    public void testOptionalClientAuth() throws Exception {
        String value = randomFrom(SSLClientAuth.OPTIONAL.name(), SSLClientAuth.OPTIONAL.name().toLowerCase(Locale.ROOT));
        SecurityNetty4Transport transport =
                createTransport(true, Settings.builder().put(SecurityNetty4Transport.CLIENT_AUTH_SETTING.getKey(), value).build());
        Netty4MockUtil.setOpenChannelsHandlerToMock(transport);
        ChannelHandler handler = transport.getServerChannelInitializer("client", Settings.EMPTY);
        final EmbeddedChannel ch = new EmbeddedChannel(handler);
        assertThat(ch.pipeline().get(SslHandler.class).engine().getNeedClientAuth(), is(false));
        assertThat(ch.pipeline().get(SslHandler.class).engine().getWantClientAuth(), is(true));
    }

    public void testProfileRequiredClientAuth() throws Exception {
        String value = randomFrom(SSLClientAuth.REQUIRED.name(), SSLClientAuth.REQUIRED.name().toLowerCase(Locale.ROOT), "true", "TRUE");
        SecurityNetty4Transport transport = createTransport(true);
        Netty4MockUtil.setOpenChannelsHandlerToMock(transport);
        ChannelHandler handler = transport.getServerChannelInitializer("client",
                Settings.builder().put(SecurityNetty4Transport.PROFILE_CLIENT_AUTH_SETTING, value).build());
        final EmbeddedChannel ch = new EmbeddedChannel(handler);
        assertThat(ch.pipeline().get(SslHandler.class).engine().getNeedClientAuth(), is(true));
        assertThat(ch.pipeline().get(SslHandler.class).engine().getWantClientAuth(), is(false));
    }

    public void testProfileNoClientAuth() throws Exception {
        String value = randomFrom(SSLClientAuth.NO.name(), "false", "FALSE", SSLClientAuth.NO.name().toLowerCase(Locale.ROOT));
        SecurityNetty4Transport transport = createTransport(true);
        Netty4MockUtil.setOpenChannelsHandlerToMock(transport);
        ChannelHandler handler = transport.getServerChannelInitializer("client",
                Settings.builder().put(SecurityNetty4Transport.PROFILE_CLIENT_AUTH_SETTING.getKey(), value).build());
        final EmbeddedChannel ch = new EmbeddedChannel(handler);
        assertThat(ch.pipeline().get(SslHandler.class).engine().getNeedClientAuth(), is(false));
        assertThat(ch.pipeline().get(SslHandler.class).engine().getWantClientAuth(), is(false));
    }

    public void testProfileOptionalClientAuth() throws Exception {
        String value = randomFrom(SSLClientAuth.OPTIONAL.name(), SSLClientAuth.OPTIONAL.name().toLowerCase(Locale.ROOT));
        SecurityNetty4Transport transport = createTransport(true);
        Netty4MockUtil.setOpenChannelsHandlerToMock(transport);
        final ChannelHandler handler = transport.getServerChannelInitializer("client",
                Settings.builder().put(SecurityNetty4Transport.PROFILE_CLIENT_AUTH_SETTING.getKey(), value).build());
        final EmbeddedChannel ch = new EmbeddedChannel(handler);
        assertThat(ch.pipeline().get(SslHandler.class).engine().getNeedClientAuth(), is(false));
        assertThat(ch.pipeline().get(SslHandler.class).engine().getWantClientAuth(), is(true));
    }

    public void testThatExceptionIsThrownWhenConfiguredWithoutSslKey() throws Exception {
        Settings settings = Settings.builder()
                .put("xpack.security.ssl.truststore.path",
                        getDataPath("/org/elasticsearch/xpack/security/transport/ssl/certs/simple/testnode.jks"))
                .put("xpack.security.ssl.truststore.password", "testnode")
                .put(SecurityNetty4Transport.SSL_SETTING.getKey(), true)
                .put("path.home", createTempDir())
                .build();
        env = new Environment(settings);
        sslService = new SSLService(settings, env);
        SecurityNetty4Transport transport = new SecurityNetty4Transport(settings, mock(ThreadPool.class), mock(NetworkService.class),
                mock(BigArrays.class), mock(NamedWriteableRegistry.class), mock(CircuitBreakerService.class), null, sslService);
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class,
                () -> transport.getServerChannelInitializer(randomAsciiOfLength(6), Settings.EMPTY));
        assertThat(e.getMessage(), containsString("key must be provided"));
    }

    public void testNoExceptionWhenConfiguredWithoutSslKeySSLDisabled() throws Exception {
        Settings settings = Settings.builder()
                .put("xpack.security.ssl.truststore.path",
                        getDataPath("/org/elasticsearch/xpack/security/transport/ssl/certs/simple/testnode.jks"))
                .put("xpack.security.ssl.truststore.password", "testnode")
                .put(SecurityNetty4Transport.SSL_SETTING.getKey(), false)
                .put("path.home", createTempDir())
                .build();
        env = new Environment(settings);
        sslService = new SSLService(settings, env);
        SecurityNetty4Transport transport = new SecurityNetty4Transport(settings, mock(ThreadPool.class), mock(NetworkService.class),
                mock(BigArrays.class), mock(NamedWriteableRegistry.class), mock(CircuitBreakerService.class), null, sslService);
        assertNotNull(transport.getServerChannelInitializer(randomAsciiOfLength(6), Settings.EMPTY));
    }
}
