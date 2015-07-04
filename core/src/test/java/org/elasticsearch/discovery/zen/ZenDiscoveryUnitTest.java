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

package org.elasticsearch.discovery.zen;

import org.elasticsearch.Version;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.settings.DynamicSettings;
import org.elasticsearch.common.network.NetworkService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.DummyTransportAddress;
import org.elasticsearch.discovery.DiscoverySettings;
import org.elasticsearch.discovery.zen.elect.ElectMasterService;
import org.elasticsearch.discovery.zen.membership.MembershipAction;
import org.elasticsearch.discovery.zen.ping.ZenPingService;
import org.elasticsearch.node.settings.NodeSettingsService;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.elasticsearch.test.cluster.TestClusterService;
import org.elasticsearch.test.transport.CapturingTransport;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.junit.After;
import org.junit.Before;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.elasticsearch.discovery.zen.ZenDiscovery.ProcessClusterState;
import static org.elasticsearch.discovery.zen.ZenDiscovery.shouldIgnoreOrRejectNewClusterState;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.IsNull.nullValue;

/**
 */
public class ZenDiscoveryUnitTest extends ElasticsearchTestCase {

    ThreadPool threadPool;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        threadPool = new ThreadPool("ZenDiscoveryUnitTest");
    }

    @After
    public void tearDown() throws Exception {
        ThreadPool.terminate(threadPool, 30, TimeUnit.SECONDS);
        super.tearDown();
    }

    public void testClusterStateChangeExistingNodeJoin() throws InterruptedException {
        // when an existing node joins, it is important that we publish a new cluster state
        // even if no node was added. The reason is that the joining node needs to recieve
        // a cluster state to finalize the join.

        final NodeSettingsService nodeSettingsService = new NodeSettingsService(Settings.EMPTY);
        final TransportService transportService = new TransportService(new CapturingTransport(), null);
        final ElectMasterService electMasterService = new ElectMasterService(Settings.EMPTY, Version.CURRENT);
        final ZenPingService pingService = new ZenPingService(Settings.EMPTY, threadPool, transportService, ClusterName.DEFAULT, new NetworkService(Settings.EMPTY), Version.CURRENT, electMasterService, null);
        final TestClusterService clusterService = new TestClusterService();
        ZenDiscovery zenDiscovery = new ZenDiscovery(Settings.EMPTY, ClusterName.DEFAULT, threadPool, transportService,
                clusterService, nodeSettingsService, pingService, electMasterService, new DiscoverySettings(Settings.EMPTY, nodeSettingsService),
                new DynamicSettings());

        ClusterState state = clusterService.state();
        final DiscoveryNodes.Builder nodesBuilder = DiscoveryNodes.builder(state.nodes());
        nodesBuilder.masterNodeId(state.nodes().localNodeId());
        final DiscoveryNode other_node = new DiscoveryNode("other_node", DummyTransportAddress.INSTANCE, Version.CURRENT);
        nodesBuilder.put(other_node);
        clusterService.setState(ClusterState.builder(state).nodes(nodesBuilder));

        state = clusterService.state();
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> exception = new AtomicReference<>();
        zenDiscovery.handleValidatedJoinRequest(other_node, new MembershipAction.JoinCallback() {
            @Override
            public void onSuccess() {
                latch.countDown();
            }

            @Override
            public void onFailure(Throwable t) {
                exception.set(t);
                logger.error("unexpected exception during join", t);
                latch.countDown();
            }
        });

        latch.await();
        if (exception.get() != null) {
            fail("unexpected exception during join: " + exception.get().getMessage());
        }

        assertTrue("failed to publish a new state upon existing join", clusterService.state() != state);
    }

    public void testShouldIgnoreNewClusterState() {
        ClusterName clusterName = new ClusterName("abc");

        DiscoveryNodes.Builder currentNodes = DiscoveryNodes.builder();
        currentNodes.masterNodeId("a");
        DiscoveryNodes.Builder newNodes = DiscoveryNodes.builder();
        newNodes.masterNodeId("a");

        ClusterState.Builder currentState = ClusterState.builder(clusterName);
        currentState.nodes(currentNodes);
        ClusterState.Builder newState = ClusterState.builder(clusterName);
        newState.nodes(newNodes);

        currentState.version(2);
        newState.version(1);
        assertTrue("should ignore, because new state's version is lower to current state's version", shouldIgnoreOrRejectNewClusterState(logger, currentState.build(), newState.build()));
        currentState.version(1);
        newState.version(1);
        assertFalse("should not ignore, because new state's version is equal to current state's version", shouldIgnoreOrRejectNewClusterState(logger, currentState.build(), newState.build()));
        currentState.version(1);
        newState.version(2);
        assertFalse("should not ignore, because new state's version is higher to current state's version", shouldIgnoreOrRejectNewClusterState(logger, currentState.build(), newState.build()));

        currentNodes = DiscoveryNodes.builder();
        currentNodes.masterNodeId("b");
        // version isn't taken into account, so randomize it to ensure this.
        if (randomBoolean()) {
            currentState.version(2);
            newState.version(1);
        } else {
            currentState.version(1);
            newState.version(2);
        }
        currentState.nodes(currentNodes);
        try {
            shouldIgnoreOrRejectNewClusterState(logger, currentState.build(), newState.build());
            fail("should ignore, because current state's master is not equal to new state's master");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("cluster state from a different master then the current one, rejecting"));
        }

        currentNodes = DiscoveryNodes.builder();
        currentNodes.masterNodeId(null);
        currentState.nodes(currentNodes);
        // version isn't taken into account, so randomize it to ensure this.
        if (randomBoolean()) {
            currentState.version(2);
            newState.version(1);
        } else {
            currentState.version(1);
            newState.version(2);
        }
        assertFalse("should not ignore, because current state doesn't have a master", shouldIgnoreOrRejectNewClusterState(logger, currentState.build(), newState.build()));
    }

    public void testSelectNextStateToProcess_empty() {
        Queue<ProcessClusterState> queue = new LinkedList<>();
        assertThat(ZenDiscovery.selectNextStateToProcess(queue), nullValue());
    }

    public void testSelectNextStateToProcess() {
        ClusterName clusterName = new ClusterName("abc");
        DiscoveryNodes nodes = DiscoveryNodes.builder().masterNodeId("a").build();

        int numUpdates = scaledRandomIntBetween(50, 100);
        LinkedList<ProcessClusterState> queue = new LinkedList<>();
        for (int i = 0; i < numUpdates; i++) {
            queue.add(new ProcessClusterState(ClusterState.builder(clusterName).version(i).nodes(nodes).build()));
        }
        ProcessClusterState mostRecent = queue.get(numUpdates - 1);
        Collections.shuffle(queue, getRandom());

        assertThat(ZenDiscovery.selectNextStateToProcess(queue), sameInstance(mostRecent.clusterState));
        assertThat(mostRecent.processed, is(true));
        assertThat(queue.size(), equalTo(0));
    }

    public void testSelectNextStateToProcess_differentMasters() {
        ClusterName clusterName = new ClusterName("abc");
        DiscoveryNodes nodes1 = DiscoveryNodes.builder().masterNodeId("a").build();
        DiscoveryNodes nodes2 = DiscoveryNodes.builder().masterNodeId("b").build();

        LinkedList<ProcessClusterState> queue = new LinkedList<>();
        ProcessClusterState thirdMostRecent = new ProcessClusterState(ClusterState.builder(clusterName).version(1).nodes(nodes1).build());
        queue.offer(thirdMostRecent);
        ProcessClusterState secondMostRecent = new ProcessClusterState(ClusterState.builder(clusterName).version(2).nodes(nodes1).build());
        queue.offer(secondMostRecent);
        ProcessClusterState mostRecent = new ProcessClusterState(ClusterState.builder(clusterName).version(3).nodes(nodes1).build());
        queue.offer(mostRecent);
        Collections.shuffle(queue, getRandom());
        queue.offer(new ProcessClusterState(ClusterState.builder(clusterName).version(4).nodes(nodes2).build()));
        queue.offer(new ProcessClusterState(ClusterState.builder(clusterName).version(5).nodes(nodes1).build()));


        assertThat(ZenDiscovery.selectNextStateToProcess(queue), sameInstance(mostRecent.clusterState));
        assertThat(thirdMostRecent.processed, is(true));
        assertThat(secondMostRecent.processed, is(true));
        assertThat(mostRecent.processed, is(true));
        assertThat(queue.size(), equalTo(2));
        assertThat(queue.get(0).processed, is(false));
        assertThat(queue.get(1).processed, is(false));
    }

}
