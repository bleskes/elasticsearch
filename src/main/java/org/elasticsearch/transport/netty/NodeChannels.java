package org.elasticsearch.transport.netty;/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.google.common.collect.ImmutableList;
import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.math.MathUtils;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.transport.ConnectTransportException;
import org.elasticsearch.transport.TransportRequestOptions;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.DefaultChannelFuture;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public interface NodeChannels {

    /** return true if channel is part of this NodeChannels */
    boolean hasChannel(Channel channel);

    /** get the channel for the requested operation type */
    Channel channel(TransportRequestOptions.Type type);

    void close();

    boolean isLight();

    public interface ChannelCloseListener {

        void onChannelClose(Channel channel, DiscoveryNode node);
    }

    public static class Full implements NodeChannels {
        private final ClientBootstrap clientBootstrap;
        private final TimeValue connectTimeout;
        private Channel[] recovery;
        private final AtomicInteger recoveryCounter = new AtomicInteger();
        private Channel[] bulk;
        private final AtomicInteger bulkCounter = new AtomicInteger();
        private Channel[] reg;
        private final AtomicInteger regCounter = new AtomicInteger();
        private Channel[] state;
        private final AtomicInteger stateCounter = new AtomicInteger();
        private Channel[] ping;
        private final AtomicInteger pingCounter = new AtomicInteger();

        public Full(ClientBootstrap clientBootstrap, TimeValue connectTimeout, int connectionsPerNodeRecovery, int connectionsPerNodeBulk, int connectionsPerNodeReg, int connectionsPerNodeState, int connectionsPerNodePing) {
            assert connectionsPerNodePing > 0;  // we rely on this when upgrading light connections
            this.clientBootstrap = clientBootstrap;
            this.connectTimeout = connectTimeout;
            this.recovery = new Channel[connectionsPerNodeRecovery];
            this.bulk = new Channel[connectionsPerNodeBulk];
            this.reg = new Channel[connectionsPerNodeReg];
            this.state = new Channel[connectionsPerNodeState];
            this.ping = new Channel[connectionsPerNodePing];
        }

        public boolean hasChannel(Channel channel) {
            return hasChannel(channel, recovery) || hasChannel(channel, bulk) || hasChannel(channel, reg) || hasChannel(channel, state) || hasChannel(channel, ping);
        }

        private boolean hasChannel(Channel channel, Channel[] channels) {
            for (Channel channel1 : channels) {
                if (channel.equals(channel1)) {
                    return true;
                }
            }
            return false;
        }

        public Channel channel(TransportRequestOptions.Type type) {
            if (type == TransportRequestOptions.Type.REG) {
                return reg[MathUtils.mod(regCounter.incrementAndGet(), reg.length)];
            } else if (type == TransportRequestOptions.Type.STATE) {
                return state[MathUtils.mod(stateCounter.incrementAndGet(), state.length)];
            } else if (type == TransportRequestOptions.Type.PING) {
                return ping[MathUtils.mod(pingCounter.incrementAndGet(), ping.length)];
            } else if (type == TransportRequestOptions.Type.BULK) {
                return bulk[MathUtils.mod(bulkCounter.incrementAndGet(), bulk.length)];
            } else if (type == TransportRequestOptions.Type.RECOVERY) {
                return recovery[MathUtils.mod(recoveryCounter.incrementAndGet(), recovery.length)];
            } else {
                throw new ElasticsearchIllegalArgumentException("no type channel for [" + type + "]");
            }
        }

        public synchronized void close() {
            List<ChannelFuture> futures = new ArrayList<>();
            closeChannelsAndWait(recovery, futures);
            closeChannelsAndWait(bulk, futures);
            closeChannelsAndWait(reg, futures);
            closeChannelsAndWait(state, futures);
            closeChannelsAndWait(ping, futures);
            for (ChannelFuture future : futures) {
                future.awaitUninterruptibly();
            }
        }

        @Override
        public boolean isLight() {
            return false;
        }

        private void closeChannelsAndWait(Channel[] channels, List<ChannelFuture> futures) {
            for (Channel channel : channels) {
                try {
                    if (channel != null && channel.isOpen()) {
                        futures.add(channel.close());
                    }
                } catch (Exception e) {
                    //ignore
                }
            }
        }

        /**
         * connect to node, with a listener for channel disconnects
         *
         * @param node                  node to connect to
         * @param existingLightChannels exiting {@Light} to the node for channels to be reused. Null if none.
         * @param channelCloseListener  listener to hook up to the channel close future
         */
        public void connectToNode(final DiscoveryNode node, @Nullable Light existingLightChannels, final ChannelCloseListener channelCloseListener) {
            ChannelFuture[] connectRecovery = new ChannelFuture[recovery.length];
            ChannelFuture[] connectBulk = new ChannelFuture[bulk.length];
            ChannelFuture[] connectReg = new ChannelFuture[reg.length];
            ChannelFuture[] connectState = new ChannelFuture[state.length];
            ChannelFuture[] connectPing = new ChannelFuture[ping.length];
            InetSocketAddress address = ((InetSocketTransportAddress) node.address()).address();

            // we try to reuse the light channel, if there. Note that ping.length is guaranteed to be >= 1
            if (existingLightChannels == null) {
                connectPing[0] = clientBootstrap.connect(address);
            } else {
                // we construct a future and mark it as success as the channel is open..
                connectPing[0] = new DefaultChannelFuture(existingLightChannels.getChannel(), false);
                connectPing[0].setSuccess();
            }
            for (int i = 1; i < connectPing.length; i++) {
                connectPing[i] = clientBootstrap.connect(address);
            }

            for (int i = 0; i < connectRecovery.length; i++) {
                connectRecovery[i] = clientBootstrap.connect(address);
            }
            for (int i = 0; i < connectBulk.length; i++) {
                connectBulk[i] = clientBootstrap.connect(address);
            }
            for (int i = 0; i < connectReg.length; i++) {
                connectReg[i] = clientBootstrap.connect(address);
            }
            for (int i = 0; i < connectState.length; i++) {
                connectState[i] = clientBootstrap.connect(address);
            }

            ChannelFutureListener closeListenerWrapper = new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    channelCloseListener.onChannelClose(future.getChannel(), node);
                }
            };

            try {
                for (int i = 0; i < connectRecovery.length; i++) {
                    connectRecovery[i].awaitUninterruptibly((long) (connectTimeout.millis() * 1.5));
                    if (!connectRecovery[i].isSuccess()) {
                        throw new ConnectTransportException(node, "connect_timeout[" + connectTimeout + "]", connectRecovery[i].getCause());
                    }
                    recovery[i] = connectRecovery[i].getChannel();
                    recovery[i].getCloseFuture().addListener(closeListenerWrapper);
                }

                for (int i = 0; i < connectBulk.length; i++) {
                    connectBulk[i].awaitUninterruptibly((long) (connectTimeout.millis() * 1.5));
                    if (!connectBulk[i].isSuccess()) {
                        throw new ConnectTransportException(node, "connect_timeout[" + connectTimeout + "]", connectBulk[i].getCause());
                    }
                    bulk[i] = connectBulk[i].getChannel();
                    bulk[i].getCloseFuture().addListener(closeListenerWrapper);
                }

                for (int i = 0; i < connectReg.length; i++) {
                    connectReg[i].awaitUninterruptibly((long) (connectTimeout.millis() * 1.5));
                    if (!connectReg[i].isSuccess()) {
                        throw new ConnectTransportException(node, "connect_timeout[" + connectTimeout + "]", connectReg[i].getCause());
                    }
                    reg[i] = connectReg[i].getChannel();
                    reg[i].getCloseFuture().addListener(closeListenerWrapper);
                }

                for (int i = 0; i < connectState.length; i++) {
                    connectState[i].awaitUninterruptibly((long) (connectTimeout.millis() * 1.5));
                    if (!connectState[i].isSuccess()) {
                        throw new ConnectTransportException(node, "connect_timeout[" + connectTimeout + "]", connectState[i].getCause());
                    }
                    state[i] = connectState[i].getChannel();
                    state[i].getCloseFuture().addListener(closeListenerWrapper);
                }

                for (int i = 0; i < connectPing.length; i++) {
                    connectPing[i].awaitUninterruptibly((long) (connectTimeout.millis() * 1.5));
                    if (!connectPing[i].isSuccess()) {
                        throw new ConnectTransportException(node, "connect_timeout[" + connectTimeout + "]", connectPing[i].getCause());
                    }
                    ping[i] = connectPing[i].getChannel();
                    if (existingLightChannels == null || i > 0) {
                        // if we reused an existing channel, no need to add another listener
                        ping[i].getCloseFuture().addListener(closeListenerWrapper);
                    }
                }

                if (recovery.length == 0) {
                    if (bulk.length > 0) {
                        recovery = bulk;
                    } else {
                        recovery = reg;
                    }
                }
                if (bulk.length == 0) {
                    bulk = reg;
                }
            } catch (RuntimeException e) {
                // clean the futures
                for (ChannelFuture future : ImmutableList.<ChannelFuture>builder().add(connectRecovery).add(connectBulk).add(connectReg).add(connectState).add(connectPing).build()) {
                    future.cancel();
                    if (future.getChannel() != null && future.getChannel().isOpen()) {
                        try {
                            future.getChannel().close();
                        } catch (Exception e1) {
                            // ignore
                        }
                    }
                }
                throw e;
            }
        }
    }

    public static class Light implements NodeChannels {
        private final ClientBootstrap clientBootstrap;
        private final TimeValue connectTimeout;

        private Channel channel;

        public Light(ClientBootstrap clientBootstrap, TimeValue connectTimeout) {
            this.clientBootstrap = clientBootstrap;
            this.connectTimeout = connectTimeout;
            this.channel = null;
        }


        @Override
        public boolean hasChannel(Channel channel) {
            return Objects.equals(channel, this.channel);
        }

        public Channel getChannel() {
            return channel;
        }

        @Override
        public Channel channel(TransportRequestOptions.Type type) {
            return channel;
        }

        /** connect to node, with a listener for channel disconnects */
        public void connectToNode(final DiscoveryNode node, final ChannelCloseListener channelCloseListener) {
            InetSocketAddress address = ((InetSocketTransportAddress) node.address()).address();
            ChannelFuture connect = clientBootstrap.connect(address);
            connect.awaitUninterruptibly((long) (connectTimeout.millis() * 1.5));
            if (!connect.isSuccess()) {
                throw new ConnectTransportException(node, "connect_timeout[" + connectTimeout + "]", connect.getCause());
            }
            channel = connect.getChannel();
            channel.getCloseFuture().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    channelCloseListener.onChannelClose(future.getChannel(), node);
                }
            });
        }

        @Override
        public void close() {
            try {
                if (channel != null && channel.isOpen()) {
                    channel.close().awaitUninterruptibly();
                }
            } catch (Exception e) {
                //ignore
            }
        }

        @Override
        public boolean isLight() {
            return true;
        }
    }
}
