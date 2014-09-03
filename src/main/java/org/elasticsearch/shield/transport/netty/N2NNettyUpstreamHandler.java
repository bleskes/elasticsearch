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
import org.elasticsearch.common.netty.channel.ChannelEvent;
import org.elasticsearch.common.netty.channel.ChannelHandler;
import org.elasticsearch.common.netty.channel.ChannelHandlerContext;
import org.elasticsearch.common.netty.handler.ipfilter.IpFilteringHandlerImpl;
import org.elasticsearch.shield.transport.n2n.IPFilteringN2NAuthenticator;

import java.net.InetSocketAddress;

/**
 *
 */
@ChannelHandler.Sharable
public class N2NNettyUpstreamHandler extends IpFilteringHandlerImpl {

    private IPFilteringN2NAuthenticator authenticator;

    @Inject
    public N2NNettyUpstreamHandler(IPFilteringN2NAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    @Override
    protected boolean accept(ChannelHandlerContext channelHandlerContext, ChannelEvent channelEvent, InetSocketAddress inetSocketAddress) throws Exception {
        // at this stage no auth has happened, so we do not have any principal anyway
        return authenticator.authenticate(null, inetSocketAddress.getAddress(), inetSocketAddress.getPort());
    }

}
