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

import org.elasticsearch.xpack.security.transport.filter.IPFilter;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.ipfilter.IpFilteringHandlerImpl;

import java.net.InetSocketAddress;

/**
 *
 */
@ChannelHandler.Sharable
public class IPFilterNetty3UpstreamHandler extends IpFilteringHandlerImpl {

    private final IPFilter filter;
    private final String profile;

    public IPFilterNetty3UpstreamHandler(IPFilter filter, String profile) {
        this.filter = filter;
        this.profile = profile;
    }

    @Override
    protected boolean accept(ChannelHandlerContext channelHandlerContext, ChannelEvent channelEvent, InetSocketAddress inetSocketAddress)
            throws Exception {
        // at this stage no auth has happened, so we do not have any principal anyway
        return filter.accept(profile, inetSocketAddress.getAddress());
    }

}
