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

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ipfilter.AbstractRemoteAddressFilter;
import org.elasticsearch.xpack.security.transport.filter.IPFilter;

import java.net.InetSocketAddress;

/**
 *
 */
public class IPFilterNetty4Handler extends AbstractRemoteAddressFilter<InetSocketAddress> {

    private final IPFilter filter;
    private final String profile;

    public IPFilterNetty4Handler(IPFilter filter, String profile) {
        this.filter = filter;
        this.profile = profile;
    }

    @Override
    protected boolean accept(ChannelHandlerContext ctx, InetSocketAddress inetSocketAddress) throws Exception {
        return filter.accept(profile, inetSocketAddress.getAddress());
    }
}
