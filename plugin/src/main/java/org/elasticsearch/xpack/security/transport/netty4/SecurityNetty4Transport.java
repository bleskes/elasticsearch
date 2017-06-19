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

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.ssl.SslHandler;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.network.NetworkService;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.BigArrays;
import org.elasticsearch.indices.breaker.CircuitBreakerService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportSettings;
import org.elasticsearch.transport.netty4.Netty4Transport;
import org.elasticsearch.xpack.ssl.SSLService;
import org.elasticsearch.xpack.security.transport.filter.IPFilter;

import javax.net.ssl.SSLEngine;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import static org.elasticsearch.xpack.security.Security.setting;
import static org.elasticsearch.xpack.security.transport.SSLExceptionHelper.isCloseDuringHandshakeException;
import static org.elasticsearch.xpack.security.transport.SSLExceptionHelper.isNotSslRecordException;
import static org.elasticsearch.xpack.security.transport.SSLExceptionHelper.isReceivedCertificateUnknownException;
import static org.elasticsearch.xpack.XPackSettings.TRANSPORT_SSL_ENABLED;

/**
 * Implementation of a transport that extends the {@link Netty4Transport} to add SSL and IP Filtering
 */
public class SecurityNetty4Transport extends Netty4Transport {

    private static final Setting<Boolean> PROFILE_SSL_SETTING = Setting.boolSetting(setting("ssl.enabled"), false);

    private final SSLService sslService;
    @Nullable private final IPFilter authenticator;
    private final Settings transportSSLSettings;
    private final boolean ssl;

    public SecurityNetty4Transport(Settings settings, ThreadPool threadPool, NetworkService networkService, BigArrays bigArrays,
                                   NamedWriteableRegistry namedWriteableRegistry, CircuitBreakerService circuitBreakerService,
                                   @Nullable IPFilter authenticator, SSLService sslService) {
        super(settings, threadPool, networkService, bigArrays, namedWriteableRegistry, circuitBreakerService);
        this.authenticator = authenticator;
        this.ssl = TRANSPORT_SSL_ENABLED.get(settings);
        this.sslService = sslService;
        this.transportSSLSettings = settings.getByPrefix(setting("transport.ssl."));
    }

    @Override
    protected void doStart() {
        super.doStart();
        if (authenticator != null) {
            authenticator.setBoundTransportAddress(boundAddress(), profileBoundAddresses());
        }
    }

    @Override
    protected ChannelHandler getServerChannelInitializer(String name, Settings settings) {
        return new SecurityServerChannelInitializer(name, settings);
    }

    @Override
    protected ChannelHandler getClientChannelInitializer() {
        return new SecurityClientChannelInitializer();
    }

    @Override
    protected void onException(Channel channel, Exception e) throws IOException {
        if (!lifecycle.started()) {
            // just close and ignore - we are already stopped and just need to make sure we release all resources
            closeChannelWhileHandlingExceptions(channel);
        } else if (isNotSslRecordException(e)) {
            if (logger.isTraceEnabled()) {
                logger.trace(
                        new ParameterizedMessage("received plaintext traffic on an encrypted channel, closing connection {}", channel), e);
            } else {
                logger.warn("received plaintext traffic on an encrypted channel, closing connection {}", channel);
            }
            closeChannelWhileHandlingExceptions(channel);
        } else if (isCloseDuringHandshakeException(e)) {
            if (logger.isTraceEnabled()) {
                logger.trace(new ParameterizedMessage("connection {} closed during ssl handshake", channel), e);
            } else {
                logger.warn("connection {} closed during handshake", channel);
            }
            closeChannelWhileHandlingExceptions(channel);
        } else if (isReceivedCertificateUnknownException(e)) {
            if (logger.isTraceEnabled()) {
                logger.trace(new ParameterizedMessage("client did not trust server's certificate, closing connection {}", channel), e);
            } else {
                logger.warn("client did not trust this server's certificate, closing connection {}", channel);
            }
            closeChannelWhileHandlingExceptions(channel);
        } else {
            super.onException(channel, e);
        }
    }

    class SecurityServerChannelInitializer extends ServerChannelInitializer {

        private final boolean sslEnabled;
        private final Settings securityProfileSettings;

        SecurityServerChannelInitializer(String name, Settings profileSettings) {
            super(name, profileSettings);
            this.sslEnabled = isProfileSSLEnabled(profileSettings, ssl);
            this.securityProfileSettings = profileSettings.getByPrefix(setting("ssl."));
            if (sslEnabled && sslService.isConfigurationValidForServerUsage(securityProfileSettings, transportSSLSettings) == false) {
                if (TransportSettings.DEFAULT_PROFILE.equals(name)) {
                    throw new IllegalArgumentException("a key must be provided to run as a server. the key should be configured using the "
                            + "[xpack.security.transport.ssl.key] or [xpack.security.transport.ssl.keystore.path] setting");
                }
                throw new IllegalArgumentException("a key must be provided to run as a server. the key should be configured using the "
                        + "[transport.profiles." + name + ".xpack.security.ssl.key] or [transport.profiles." + name
                        + ".xpack.security.ssl.keystore.path] setting");
            }
        }

        @Override
        protected void initChannel(Channel ch) throws Exception {
            super.initChannel(ch);
            if (sslEnabled) {
                SSLEngine serverEngine = sslService.createSSLEngine(securityProfileSettings, transportSSLSettings);
                serverEngine.setUseClientMode(false);
                ch.pipeline().addFirst(new SslHandler(serverEngine));
            }
            if (authenticator != null) {
                ch.pipeline().addFirst(new IpFilterRemoteAddressFilter(authenticator, name));
            }
        }
    }

    public static boolean isProfileSSLEnabled(Settings profileSettings, boolean defaultTransportSSL) {
        return PROFILE_SSL_SETTING.exists(profileSettings) ? PROFILE_SSL_SETTING.get(profileSettings) : defaultTransportSSL;
    }

    private class SecurityClientChannelInitializer extends ClientChannelInitializer {

        private final boolean hostnameVerificationEnabled;

        SecurityClientChannelInitializer() {
            this.hostnameVerificationEnabled =
                    sslService.getVerificationMode(transportSSLSettings, Settings.EMPTY).isHostnameVerificationEnabled();
        }

        @Override
        protected void initChannel(Channel ch) throws Exception {
            super.initChannel(ch);
            if (ssl) {
                ch.pipeline().addFirst(new ClientSslHandlerInitializer(transportSSLSettings, sslService, hostnameVerificationEnabled));
            }
        }
    }

    private static class ClientSslHandlerInitializer extends ChannelOutboundHandlerAdapter {

        private final boolean hostnameVerificationEnabled;
        private final Settings sslSettings;
        private final SSLService sslService;

        private ClientSslHandlerInitializer(Settings sslSettings, SSLService sslService, boolean hostnameVerificationEnabled) {
            this.sslSettings = sslSettings;
            this.hostnameVerificationEnabled = hostnameVerificationEnabled;
            this.sslService = sslService;
        }

        @Override
        public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress,
                            SocketAddress localAddress, ChannelPromise promise) throws Exception {
            final SSLEngine sslEngine;
            if (hostnameVerificationEnabled) {
                InetSocketAddress inetSocketAddress = (InetSocketAddress) remoteAddress;
                // we create the socket based on the name given. don't reverse DNS
                sslEngine = sslService.createSSLEngine(sslSettings, Settings.EMPTY, inetSocketAddress.getHostString(),
                        inetSocketAddress.getPort());
            } else {
                sslEngine = sslService.createSSLEngine(sslSettings, Settings.EMPTY);
            }

            sslEngine.setUseClientMode(true);
            ctx.pipeline().replace(this, "ssl", new SslHandler(sslEngine));
            super.connect(ctx, remoteAddress, localAddress, promise);
        }
    }
}
