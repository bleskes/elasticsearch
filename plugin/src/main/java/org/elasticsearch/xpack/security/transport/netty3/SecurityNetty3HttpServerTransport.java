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
import org.elasticsearch.common.network.NetworkService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.BigArrays;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.http.HttpServerTransport;
import org.elasticsearch.http.netty3.Netty3HttpServerTransport;
import org.elasticsearch.transport.netty3.Netty3Utils;
import org.elasticsearch.xpack.ssl.SSLService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.security.transport.filter.IPFilter;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLEngine;

import static org.elasticsearch.http.HttpTransportSettings.SETTING_HTTP_COMPRESSION;
import static org.elasticsearch.xpack.security.transport.SSLExceptionHelper.isCloseDuringHandshakeException;
import static org.elasticsearch.xpack.security.transport.SSLExceptionHelper.isNotSslRecordException;
import static org.elasticsearch.xpack.XPackSettings.HTTP_SSL_ENABLED;

public class SecurityNetty3HttpServerTransport extends Netty3HttpServerTransport {

    private final IPFilter ipFilter;
    private final SSLService sslService;
    private final boolean ssl;

    public SecurityNetty3HttpServerTransport(Settings settings, NetworkService networkService, BigArrays bigArrays, IPFilter ipFilter,
                                             SSLService sslService, ThreadPool threadPool, NamedXContentRegistry xContentRegistry,
                                             HttpServerTransport.Dispatcher dispatcher) {
        super(settings, networkService, bigArrays, threadPool, xContentRegistry, dispatcher);
        deprecationLogger.deprecated("http type [security3] is deprecated");
        this.ipFilter = ipFilter;
        this.sslService =  sslService;
        this.ssl = HTTP_SSL_ENABLED.get(settings);
    }

    @Override
    protected void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        Netty3Utils.maybeDie(e.getCause());
        if (!lifecycle.started()) {
            return;
        }

        Throwable t = e.getCause();
        if (isNotSslRecordException(t)) {
            if (logger.isTraceEnabled()) {
                logger.trace(
                        (Supplier<?>) () -> new ParameterizedMessage(
                                "received plaintext http traffic on a https channel, closing connection {}",
                                ctx.getChannel()),
                        t);
            } else {
                logger.warn("received plaintext http traffic on a https channel, closing connection {}", ctx.getChannel());
            }
            ctx.getChannel().close();
        } else if (isCloseDuringHandshakeException(t)) {
            if (logger.isTraceEnabled()) {
                logger.trace((Supplier<?>) () -> new ParameterizedMessage("connection {} closed during handshake", ctx.getChannel()), t);
            } else {
                logger.warn("connection {} closed during handshake", ctx.getChannel());
            }
            ctx.getChannel().close();
        } else {
            super.exceptionCaught(ctx, e);
        }
    }

    @Override
    protected void doStart() {
        super.doStart();
        ipFilter.setBoundHttpTransportAddress(this.boundAddress());
    }

    @Override
    public ChannelPipelineFactory configureServerChannelPipelineFactory() {
        return new HttpSslChannelPipelineFactory(this);
    }

    private class HttpSslChannelPipelineFactory extends HttpChannelPipelineFactory {

        private final Settings sslSettings;

        HttpSslChannelPipelineFactory(Netty3HttpServerTransport transport) {
            super(transport, detailedErrorsEnabled, threadPool.getThreadContext());
            this.sslSettings = SSLService.getHttpTransportSSLSettings(settings);
            if (ssl && sslService.isConfigurationValidForServerUsage(sslSettings, Settings.EMPTY) == false) {
                throw new IllegalArgumentException("a key must be provided to run as a server. the key should be configured using the " +
                        "[xpack.security.http.ssl.key] or [xpack.security.http.ssl.keystore.path] setting");
            }
        }

        @Override
        public ChannelPipeline getPipeline() throws Exception {
            ChannelPipeline pipeline = super.getPipeline();
            if (ssl) {
                SSLEngine engine = sslService.createSSLEngine(sslSettings, Settings.EMPTY);
                pipeline.addFirst("ssl", new SslHandler(engine));
            }
            pipeline.addFirst("ipfilter", new IPFilterNetty3UpstreamHandler(ipFilter, IPFilter.HTTP_PROFILE_NAME));
            return pipeline;
        }
    }

    public static void overrideSettings(Settings.Builder settingsBuilder, Settings settings) {
        if (HTTP_SSL_ENABLED.get(settings) && SETTING_HTTP_COMPRESSION.exists(settings) == false) {
            settingsBuilder.put(SETTING_HTTP_COMPRESSION.getKey(), false);
        }
    }
}
