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

package org.elasticsearch.xpack.security.transport.netty3;

import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.util.Supplier;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.internal.Nullable;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.network.NetworkService;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.BigArrays;
import org.elasticsearch.indices.breaker.CircuitBreakerService;
import org.elasticsearch.transport.TransportSettings;
import org.elasticsearch.xpack.ssl.SSLConfiguration;
import org.elasticsearch.xpack.ssl.SSLService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.netty3.Netty3Transport;
import org.elasticsearch.xpack.security.transport.filter.IPFilter;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.elasticsearch.xpack.security.Security.setting;
import static org.elasticsearch.xpack.security.transport.SSLExceptionHelper.isCloseDuringHandshakeException;
import static org.elasticsearch.xpack.security.transport.SSLExceptionHelper.isNotSslRecordException;
import static org.elasticsearch.xpack.XPackSettings.TRANSPORT_SSL_ENABLED;

public class SecurityNetty3Transport extends Netty3Transport {

    public static final Setting<Boolean> PROFILE_SSL_SETTING = Setting.boolSetting(setting("ssl.enabled"), false);

    private final SSLService sslService;
    @Nullable private final IPFilter authenticator;
    private final boolean defaultTransportSslEnabled;
    private final SSLConfiguration sslConfiguration;
    private final Map<String, SSLConfiguration> profileConfiguration;

    @Inject
    public SecurityNetty3Transport(Settings settings, ThreadPool threadPool, NetworkService networkService, BigArrays bigArrays,
                                   @Nullable IPFilter authenticator, SSLService sslService, NamedWriteableRegistry namedWriteableRegistry,
                                   CircuitBreakerService circuitBreakerService) {
        super(settings, threadPool, networkService, bigArrays, namedWriteableRegistry, circuitBreakerService);
        this.authenticator = authenticator;
        this.defaultTransportSslEnabled = TRANSPORT_SSL_ENABLED.get(settings);
        this.sslService = sslService;
        final Settings transportSSLSettings = settings.getByPrefix(setting("transport.ssl."));
        sslConfiguration = defaultTransportSslEnabled ? sslService.sslConfiguration(transportSSLSettings, Settings.EMPTY) : null;

        Map<String, Settings> profileSettingsMap = settings.getGroups("transport.profiles.", true);
        Map<String, SSLConfiguration> profileConfiguration = new HashMap<>(profileSettingsMap.size() + 1);

        if (sslConfiguration != null) {
            validateProfileSettings(sslService, transportSSLSettings, TransportSettings.DEFAULT_PROFILE, transportSSLSettings);
            profileConfiguration.put(TransportSettings.DEFAULT_PROFILE, sslConfiguration);
        } else {
            profileConfiguration.put(TransportSettings.DEFAULT_PROFILE, null);
        }
        for (Map.Entry<String, Settings> entry : profileSettingsMap.entrySet()) {
            Settings profileSettings = entry.getValue();
            String name = entry.getKey();
            if (isProfileSSLEnabled(profileSettings, defaultTransportSslEnabled)) {
                final Settings profileSslSettings = profileSettings.getByPrefix(setting("ssl."));
                SSLConfiguration configuration =  sslService.sslConfiguration(profileSslSettings, transportSSLSettings);
                validateProfileSettings(sslService, transportSSLSettings, name, profileSslSettings);
                profileConfiguration.put(name, configuration);
            } else {
                profileConfiguration.put(name, null);
            }
        }
        this.profileConfiguration = Collections.unmodifiableMap(profileConfiguration);
    }

    void validateProfileSettings(SSLService sslService, Settings transportSSLSettings, String name, Settings profileSslSettings) {
        if (NetworkService.NETWORK_SERVER.get(settings)) { // only validate this if we run as a server
            if (sslService.isConfigurationValidForServerUsage(profileSslSettings, transportSSLSettings) == false) {
                if (TransportSettings.DEFAULT_PROFILE.equals(name)) {
                    throw new IllegalArgumentException("a key must be provided to run as a server." +
                            " the key should be configured using the [xpack.security.transport.ssl.key]" +
                            " or [xpack.security.transport.ssl.keystore.path] setting");
                }
                throw new IllegalArgumentException("a key must be provided to run as a server. the key should be configured using the "
                        + "[transport.profiles." + name + ".xpack.security.ssl.key] or [transport.profiles." + name
                        + ".xpack.security.ssl.keystore.path] setting");
            }
        }
    }

    @Override
    protected String deprecationMessage() {
        return "transport type [security3] is deprecated";
    }

    @Override
    protected void doStart() {
        super.doStart();
        if (authenticator != null) {
            authenticator.setBoundTransportAddress(boundAddress(), profileBoundAddresses());
        }
    }

    @Override
    public ChannelPipelineFactory configureClientChannelPipelineFactory() {
        return new SslClientChannelPipelineFactory();
    }

    @Override
    public ChannelPipelineFactory configureServerChannelPipelineFactory(String name, Settings profileSettings) {
        if (profileConfiguration.containsKey(name) == false) {
            // we have null values for non-ssl profiles
            throw new IllegalStateException("unknown profile: " + name);
        }
        SSLConfiguration configuration = profileConfiguration.get(name);
        return new SslServerChannelPipelineFactory(name, settings, configuration);
    }

    @Override
    protected void onException(Channel channel, Exception e) throws IOException {
        if (isNotSslRecordException(e)) {
            if (logger.isTraceEnabled()) {
                logger.trace(
                        (Supplier<?>) () -> new ParameterizedMessage(
                                "received plaintext traffic on a encrypted channel, closing connection {}", channel), e);
            } else {
                logger.warn("received plaintext traffic on a encrypted channel, closing connection {}", channel);
            }
            closeChannelWhileHandlingExceptions(channel);
        } else if (isCloseDuringHandshakeException(e)) {
            if (logger.isTraceEnabled()) {
                logger.trace((Supplier<?>) () -> new ParameterizedMessage("connection {} closed during handshake", channel), e);
            } else {
                logger.warn("connection {} closed during handshake", channel);
            }
            closeChannelWhileHandlingExceptions(channel);
        } else {
            super.onException(channel, e);
        }
    }

    public static Settings profileSslSettings(Settings profileSettings) {
        return profileSettings.getByPrefix(setting("ssl."));
    }

    private class SslServerChannelPipelineFactory extends ServerChannelPipelineFactory {
        private final SSLConfiguration configuration;

        SslServerChannelPipelineFactory(String name, Settings settings, SSLConfiguration configuration) {
            super(SecurityNetty3Transport.this, name, settings);
            this.configuration = configuration;
        }

        @Override
        public ChannelPipeline getPipeline() throws Exception {
            ChannelPipeline pipeline = super.getPipeline();
            if (configuration != null) {
                final SSLEngine serverEngine = sslService.createSSLEngine(configuration, null, -1);
                serverEngine.setUseClientMode(false);
                pipeline.addFirst("ssl", new SslHandler(serverEngine));
            }
            if (authenticator != null) {
                pipeline.addFirst("ipfilter", new IPFilterNetty3UpstreamHandler(authenticator, name));
            }
            return pipeline;
        }
    }

    private class SslClientChannelPipelineFactory extends ClientChannelPipelineFactory {

        SslClientChannelPipelineFactory() {
            super(SecurityNetty3Transport.this);
        }

        @Override
        public ChannelPipeline getPipeline() throws Exception {
            ChannelPipeline pipeline = super.getPipeline();
            if (sslConfiguration != null) {
                pipeline.addFirst("sslInitializer", new ClientSslHandlerInitializer());
            }
            return pipeline;
        }

        /**
         * Handler that waits until connect is called to create a SSLEngine with the proper parameters in order to
         * perform hostname verification
         */
        private class ClientSslHandlerInitializer extends SimpleChannelHandler {

            @Override
            public void connectRequested(ChannelHandlerContext ctx, ChannelStateEvent e) {
                SSLEngine sslEngine;
                if (sslConfiguration.verificationMode().isHostnameVerificationEnabled()) {
                    InetSocketAddress inetSocketAddress = (InetSocketAddress) e.getValue();
                    // we create the socket based on the name given. don't reverse DNS
                    sslEngine = sslService.createSSLEngine(sslConfiguration, inetSocketAddress.getHostString(),
                            inetSocketAddress.getPort());
                } else {
                    sslEngine = sslService.createSSLEngine(sslConfiguration, null, -1);
                }

                sslEngine.setUseClientMode(true);
                ctx.getPipeline().replace(this, "ssl", new SslHandler(sslEngine));
                ctx.getPipeline().addAfter("ssl", "handshake", new Netty3HandshakeWaitingHandler(logger));

                ctx.sendDownstream(e);
            }
        }
    }

    private static boolean isProfileSSLEnabled(Settings profileSettings, boolean defaultTransportSSL) {
        return PROFILE_SSL_SETTING.exists(profileSettings) ? PROFILE_SSL_SETTING.get(profileSettings) : defaultTransportSSL;
    }
}
