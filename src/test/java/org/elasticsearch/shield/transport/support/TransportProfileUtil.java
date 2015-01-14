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

package org.elasticsearch.shield.transport.support;

import org.elasticsearch.common.netty.channel.Channel;
import org.elasticsearch.test.InternalTestCluster;
import org.elasticsearch.transport.Transport;
import org.elasticsearch.transport.netty.NettyTransport;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.Map;

/**
 * Utility class used to deal with profile support in Transport. This class should be removed once
 * core has been fixed and Shield has been updated to depend on a version of core that is fixed.
 *
 * See <a href="https://github.com/elasticsearch/elasticsearch/pull/9134">https://github.com/elasticsearch/elasticsearch/pull/9134</a>
 */
//TODO remove the reflection shenanigans (actually this class as a whole) once es core dependency is upgraded for 1.5
public class TransportProfileUtil {

    private TransportProfileUtil() {}

    /*
     * Gets the actual port that the profile is listening on. If a range was provided in the settings, then the first
     * port may not be the port that was actually bound.
     */
    public static int getProfilePort(String profile, InternalTestCluster internalTestCluster) throws Exception {
        NettyTransport transport = (NettyTransport) internalTestCluster.getInstance(Transport.class);
        Field channels = NettyTransport.class.getDeclaredField("serverChannels");
        channels.setAccessible(true);
        Map<String, Channel> serverChannels = (Map<String, Channel>) channels.get(transport);
        Channel clientProfileChannel = serverChannels.get(profile);
        return ((InetSocketAddress) clientProfileChannel.getLocalAddress()).getPort();
    }
}
