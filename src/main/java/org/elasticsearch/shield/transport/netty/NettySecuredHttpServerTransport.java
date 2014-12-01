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

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Provider;
import org.elasticsearch.common.inject.internal.Nullable;
import org.elasticsearch.common.netty.channel.ChannelPipeline;
import org.elasticsearch.common.netty.channel.ChannelPipelineFactory;
import org.elasticsearch.common.netty.handler.ssl.SslHandler;
import org.elasticsearch.common.network.NetworkService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.BigArrays;
import org.elasticsearch.http.netty.NettyHttpServerTransport;
import org.elasticsearch.shield.ssl.SSLService;
import org.elasticsearch.shield.ssl.SSLServiceProvider;
import org.elasticsearch.shield.transport.n2n.IPFilteringN2NAuthenticator;

import javax.net.ssl.SSLEngine;

/**
 *
 */
public class NettySecuredHttpServerTransport extends NettyHttpServerTransport {

    private final IPFilteringN2NAuthenticator authenticator;
    private final @Nullable SSLService sslService;

    @Inject
    public NettySecuredHttpServerTransport(Settings settings, NetworkService networkService, BigArrays bigArrays,
                                           IPFilteringN2NAuthenticator authenticator, SSLServiceProvider sslServiceProvider) {
        super(settings, networkService, bigArrays);
        this.authenticator = authenticator;
        this.sslService = settings.getAsBoolean("shield.http.ssl", false) ? sslServiceProvider.get() : null;
    }

    @Override
    public ChannelPipelineFactory configureServerChannelPipelineFactory() {
        return new HttpSslChannelPipelineFactory(this);
    }

    private class HttpSslChannelPipelineFactory extends HttpChannelPipelineFactory {

        public HttpSslChannelPipelineFactory(NettyHttpServerTransport transport) {
            super(transport);
        }

        @Override
        public ChannelPipeline getPipeline() throws Exception {
            ChannelPipeline pipeline = super.getPipeline();
            if (sslService != null) {
                SSLEngine engine = sslService.createSSLEngine();
                engine.setUseClientMode(false);
                engine.setNeedClientAuth(false);

                pipeline.addFirst("ssl", new SslHandler(engine));
            }
            pipeline.addFirst("ipfilter", new N2NNettyUpstreamHandler(authenticator, "default"));
            return pipeline;
        }
    }
}
