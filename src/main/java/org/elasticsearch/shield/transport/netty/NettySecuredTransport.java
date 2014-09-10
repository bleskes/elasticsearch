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

package org.elasticsearch.shield.transport.netty;

import org.elasticsearch.Version;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.internal.Nullable;
import org.elasticsearch.common.netty.channel.ChannelPipeline;
import org.elasticsearch.common.netty.channel.ChannelPipelineFactory;
import org.elasticsearch.common.netty.handler.ssl.SslHandler;
import org.elasticsearch.common.network.NetworkService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.BigArrays;
import org.elasticsearch.shield.transport.ssl.SSLConfig;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.netty.NettyTransport;

import javax.net.ssl.SSLEngine;

/**
 *
 */
public class NettySecuredTransport extends NettyTransport {

    private final boolean ssl;
    private final N2NNettyUpstreamHandler shieldUpstreamHandler;

    @Inject
    public NettySecuredTransport(Settings settings, ThreadPool threadPool, NetworkService networkService, BigArrays bigArrays, Version version,
                                 @Nullable N2NNettyUpstreamHandler shieldUpstreamHandler) {
        super(settings, threadPool, networkService, bigArrays, version);
        this.shieldUpstreamHandler = shieldUpstreamHandler;
        this.ssl = settings.getAsBoolean("shield.transport.ssl", false);
    }

    @Override
    public ChannelPipelineFactory configureClientChannelPipelineFactory() {
        return new SslClientChannelPipelineFactory(this);
    }

    @Override
    public ChannelPipelineFactory configureServerChannelPipelineFactory() {
        return new SslServerChannelPipelineFactory(this);
    }

    private class SslServerChannelPipelineFactory extends ServerChannelPipeFactory {

        private final SSLConfig sslConfig;

        public SslServerChannelPipelineFactory(NettyTransport nettyTransport) {
            super(nettyTransport);
            if (ssl) {
                sslConfig = new SSLConfig(settings.getByPrefix("shield.transport.ssl."), settings.getByPrefix("shield.ssl."));
                // try to create an SSL engine, so that exceptions lead to early exit
                sslConfig.createSSLEngine();
            } else {
                sslConfig = null;
            }
        }

        @Override
        public ChannelPipeline getPipeline() throws Exception {
            ChannelPipeline pipeline = super.getPipeline();
            if (shieldUpstreamHandler != null) {
                pipeline.addFirst("ipfilter", shieldUpstreamHandler);
            }
            if (ssl) {
                SSLEngine serverEngine = sslConfig.createSSLEngine();
                serverEngine.setUseClientMode(false);

                pipeline.addFirst("ssl", new SslHandler(serverEngine));
                pipeline.replace("dispatcher", "dispatcher", new SecuredMessageChannelHandler(nettyTransport, logger));
            }
            return pipeline;
        }
    }

    private class SslClientChannelPipelineFactory extends ClientChannelPipelineFactory {

        private final SSLConfig sslConfig;

        public SslClientChannelPipelineFactory(NettyTransport transport) {
            super(transport);
            if (ssl) {
                sslConfig = new SSLConfig(settings.getByPrefix("shield.transport.ssl."), settings.getByPrefix("shield.ssl."));
                // try to create an SSL engine, so that exceptions lead to early exit
                sslConfig.createSSLEngine();
            } else {
               sslConfig = null;
            }
        }

        @Override
        public ChannelPipeline getPipeline() throws Exception {
            ChannelPipeline pipeline = super.getPipeline();
            if (ssl) {
                SSLEngine clientEngine = sslConfig.createSSLEngine();
                clientEngine.setUseClientMode(true);

                pipeline.addFirst("ssl", new SslHandler(clientEngine));
                pipeline.replace("dispatcher", "dispatcher", new SecuredMessageChannelHandler(nettyTransport, logger));
            }
            return pipeline;
        }
    }
}
