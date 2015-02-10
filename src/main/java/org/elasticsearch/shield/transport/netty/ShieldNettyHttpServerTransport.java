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
import org.elasticsearch.common.netty.channel.ChannelHandlerContext;
import org.elasticsearch.common.netty.channel.ChannelPipeline;
import org.elasticsearch.common.netty.channel.ChannelPipelineFactory;
import org.elasticsearch.common.netty.channel.ExceptionEvent;
import org.elasticsearch.common.netty.handler.ssl.NotSslRecordException;
import org.elasticsearch.common.netty.handler.ssl.SslHandler;
import org.elasticsearch.common.network.NetworkService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.BigArrays;
import org.elasticsearch.http.netty.NettyHttpServerTransport;
import org.elasticsearch.http.netty.VisibleNettyHttpServerTransport;
import org.elasticsearch.shield.ssl.ServerSSLService;
import org.elasticsearch.shield.transport.filter.IPFilter;

import javax.net.ssl.SSLEngine;

/**
 *
 */
public class ShieldNettyHttpServerTransport extends VisibleNettyHttpServerTransport {

    private final IPFilter ipFilter;
    private final ServerSSLService sslService;
    private final boolean ssl;

    @Inject
    public ShieldNettyHttpServerTransport(Settings settings, NetworkService networkService, BigArrays bigArrays,
                                          IPFilter ipFilter, ServerSSLService sslService) {
        super(settings, networkService, bigArrays);
        this.ipFilter = ipFilter;
        this.ssl = settings.getAsBoolean("shield.http.ssl", false);
        this.sslService =  sslService;
    }

    @Override
    protected void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        if (e.getCause() instanceof NotSslRecordException) {
            if (logger.isTraceEnabled()) {
                logger.trace("received plaintext http traffic on a https channel, closing connection {}", e.getCause(), ctx.getChannel());
            } else {
                logger.warn("received plaintext http traffic on a https channel, closing connection {}", ctx.getChannel());
            }
            ctx.getChannel().close();
        } else {
            super.exceptionCaught(ctx, e);
        }
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
            if (ssl) {
                SSLEngine engine = sslService.createSSLEngine();
                engine.setUseClientMode(false);
                engine.setNeedClientAuth(settings.getAsBoolean("shield.http.ssl.client.auth", false));

                pipeline.addFirst("ssl", new SslHandler(engine));
            }
            pipeline.addFirst("ipfilter", new IPFilterNettyUpstreamHandler(ipFilter, IPFilter.HTTP_PROFILE_NAME));
            return pipeline;
        }
    }
}
