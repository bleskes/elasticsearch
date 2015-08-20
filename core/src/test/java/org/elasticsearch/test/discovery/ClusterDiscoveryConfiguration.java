/*
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
package org.elasticsearch.test.discovery;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.google.common.primitives.Ints;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.network.NetworkUtils;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.discovery.DiscoverySettings;
import org.elasticsearch.test.InternalTestCluster;
import org.elasticsearch.test.SettingsSource;
import org.elasticsearch.transport.local.LocalTransport;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.Set;

public class ClusterDiscoveryConfiguration extends SettingsSource {

    static Settings DEFAULT_NODE_SETTINGS = Settings.settingsBuilder().put("discovery.type", "zen").build();
    private static final String IP_ADDR = "127.0.0.1";

    final int numOfNodes;
    final Settings nodeSettings;
    final Settings transportClientSettings;

    public ClusterDiscoveryConfiguration(int numOfNodes, Settings extraSettings) {
        this.numOfNodes = numOfNodes;
        this.nodeSettings = Settings.builder().put(DEFAULT_NODE_SETTINGS).put(extraSettings).build();
        this.transportClientSettings = Settings.builder().put(extraSettings).build();
    }

    @Override
    public Settings node(int nodeOrdinal) {
        return nodeSettings;
    }

    @Override
    public Settings transportClient() {
        return transportClientSettings;
    }

    public static class UnicastZen extends ClusterDiscoveryConfiguration {

        // this variable is incremented on each bind attempt and will maintain the next port that should be tried
        private static int nextPort = calcBasePort();

        private final int[] unicastHostOrdinals;
        private final int[] unicastHostPorts;
        private final String[] unicastHosts;
        private final boolean localMode;

        public UnicastZen(int numOfNodes, Settings extraSettings, InternalTestCluster cluster) {
            this(numOfNodes, numOfNodes, extraSettings, cluster);
        }

        public UnicastZen(int numOfNodes, int numOfUnicastHosts, Settings extraSettings, InternalTestCluster cluster) {
            super(numOfNodes, extraSettings);
            if (numOfUnicastHosts == numOfNodes) {
                unicastHostOrdinals = new int[numOfNodes];
                for (int i = 0; i < numOfNodes; i++) {
                    unicastHostOrdinals[i] = i;
                }
            } else {
                Set<Integer> ordinals = new HashSet<>(numOfUnicastHosts);
                while (ordinals.size() != numOfUnicastHosts) {
                    ordinals.add(RandomizedTest.randomInt(numOfNodes - 1));
                }
                unicastHostOrdinals = Ints.toArray(ordinals);
            }
            this.unicastHostPorts = unicastHostPorts(numOfNodes);
            this.localMode = cluster.isLocalTransportConfigured();
            if (localMode) {
                this.unicastHosts = buildLocalUnicastHosts(unicastHostOrdinals, cluster);
            } else {
                this.unicastHosts = buildNetworkUnicastHosts(unicastHostOrdinals, unicastHostPorts);
            }
            assert unicastHostOrdinals.length <= unicastHostPorts.length;
        }

        public UnicastZen(int numOfNodes, int[] unicastHostOrdinals, InternalTestCluster cluster) {
            this(numOfNodes, Settings.EMPTY, unicastHostOrdinals, cluster);
        }

        public UnicastZen(int numOfNodes, Settings extraSettings, int[] unicastHostOrdinals, InternalTestCluster cluster) {
            super(numOfNodes, extraSettings);
            this.unicastHostOrdinals = unicastHostOrdinals;
            this.unicastHostPorts = unicastHostPorts(numOfNodes);
            this.localMode = cluster.isLocalTransportConfigured();
            if (localMode) {
                this.unicastHosts = buildLocalUnicastHosts(unicastHostOrdinals, cluster);
            } else {
                this.unicastHosts = buildNetworkUnicastHosts(unicastHostOrdinals, unicastHostPorts);
            }
            assert unicastHostOrdinals.length <= unicastHostPorts.length;
        }

        private static int calcBasePort() {
            return 30000 + InternalTestCluster.BASE_PORT;
        }

        @Override
        public Settings node(int nodeOrdinal) {
            Settings.Builder builder = Settings.builder();
            if (nodeOrdinal >= unicastHostPorts.length) {
                throw new ElasticsearchException("nodeOrdinal [" + nodeOrdinal + "] is greater than the number unicast ports [" + unicastHostPorts.length + "]");
            } else if (localMode == false) {
                // we need to pin the node port & host so we'd know where to point things
                builder.put("transport.tcp.port", unicastHostPorts[nodeOrdinal]);
                builder.put("transport.host", IP_ADDR); // only bind on one IF we use v4 here by default
                builder.put("transport.bind_host", IP_ADDR);
                builder.put("transport.publish_host", IP_ADDR);
                builder.put("http.enabled", false);
            }
            builder.putArray(DiscoverySettings.UNICAST_HOSTS, unicastHosts);
            return builder.put(super.node(nodeOrdinal)).build();
        }

        protected static String[] buildLocalUnicastHosts(int[] unicastHostOrdinals, InternalTestCluster cluster) {
            String[] unicastHosts = new String[unicastHostOrdinals.length];
            for (int i = 0; i < unicastHostOrdinals.length; i++) {
                unicastHosts[i] = cluster.getNodeLocalAddress(i);
            }
            return unicastHosts;
        }

        protected static String[] buildNetworkUnicastHosts(int[] unicastHostOrdinals, int[] unicastHostPorts) {
            String[] unicastHosts = new String[unicastHostOrdinals.length];
            for (int i = 0; i < unicastHostOrdinals.length; i++) {
                unicastHosts[i] = IP_ADDR + ":" + (unicastHostPorts[unicastHostOrdinals[i]]);
            }
            return unicastHosts;
        }

        protected synchronized static int[] unicastHostPorts(int numHosts) {
            int[] unicastHostPorts = new int[numHosts];

            final int basePort = calcBasePort();
            final int maxPort = basePort + InternalTestCluster.PORTS_PER_JVM;
            int tries = 0;
            for (int i = 0; i < unicastHostPorts.length; i++) {
                boolean foundPortInRange = false;
                while (tries < InternalTestCluster.PORTS_PER_JVM && !foundPortInRange) {
                    try (ServerSocket serverSocket = new ServerSocket()) {
                        // Set SO_REUSEADDR as we may bind here and not be able to reuse the address immediately without it.
                        serverSocket.setReuseAddress(NetworkUtils.defaultReuseAddress());
                        serverSocket.bind(new InetSocketAddress(IP_ADDR, nextPort));
                        // bind was a success
                        foundPortInRange = true;
                        unicastHostPorts[i] = nextPort;
                    } catch (IOException e) {
                        // Do nothing
                    }

                    nextPort++;
                    if (nextPort >= maxPort) {
                        // Roll back to the beginning of the range and do not go into another JVM's port range
                        nextPort = basePort;
                    }
                    tries++;
                }

                if (!foundPortInRange) {
                    throw new ElasticsearchException("could not find enough open ports in range [" + basePort + "-" + maxPort + "]. required [" + unicastHostPorts.length + "] ports");
                }
            }
            return unicastHostPorts;
        }
    }
}
