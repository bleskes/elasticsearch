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

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.netty.channel.ChannelFuture;
import org.elasticsearch.common.netty.channel.ChannelFutureListener;
import org.elasticsearch.common.netty.channel.ChannelHandlerContext;
import org.elasticsearch.common.netty.channel.ChannelStateEvent;
import org.elasticsearch.common.netty.handler.ssl.SslHandler;
import org.elasticsearch.shield.transport.ssl.ElasticsearchSSLException;
import org.elasticsearch.transport.netty.MessageChannelHandler;

public class SecuredMessageChannelHandler extends MessageChannelHandler {

    public SecuredMessageChannelHandler(org.elasticsearch.transport.netty.NettyTransport transport, ESLogger logger) {
        super(transport, logger);
    }

    @Override
    public void channelConnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
        SslHandler sslHandler = ctx.getPipeline().get(SslHandler.class);

        // Get notified when SSL handshake is done.
        final ChannelFuture handshakeFuture = sslHandler.handshake();
        handshakeFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    logger.debug("SSL / TLS handshake completed for channel");
                    ctx.sendUpstream(e);
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.error("SSL / TLS handshake failed, closing channel: {}", future.getCause(), future.getCause().getMessage());
                    } else {
                        logger.error("SSL / TLS handshake failed, closing channel: {}", future.getCause().getMessage());
                    }
                    future.getChannel().close();
                }
            }
        });
    }
}
