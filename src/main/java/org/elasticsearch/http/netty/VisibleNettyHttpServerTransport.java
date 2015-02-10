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

package org.elasticsearch.http.netty;

import org.elasticsearch.common.netty.channel.ChannelHandlerContext;
import org.elasticsearch.common.netty.channel.ExceptionEvent;
import org.elasticsearch.common.network.NetworkService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.BigArrays;

/**
 * Makes the exceptionCaught method of {@link org.elasticsearch.http.netty.NettyHttpServerTransport} visible
 * to overriding classes.
 *
 * TODO: Fix core to make methods protected instead of package private and remove this class
 */
public class VisibleNettyHttpServerTransport extends NettyHttpServerTransport {

    public VisibleNettyHttpServerTransport(Settings settings, NetworkService networkService, BigArrays bigArrays) {
        super(settings, networkService, bigArrays);
    }

    @Override
    protected void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        super.exceptionCaught(ctx, e);
    }

}
