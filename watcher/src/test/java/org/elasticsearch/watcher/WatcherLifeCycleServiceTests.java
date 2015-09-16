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

package org.elasticsearch.watcher;

import com.google.common.util.concurrent.MoreExecutors;
import org.elasticsearch.cluster.*;
import org.elasticsearch.cluster.block.ClusterBlocks;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.gateway.GatewayService;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.threadpool.ThreadPool;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.concurrent.Executor;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 */
public class WatcherLifeCycleServiceTests extends ESTestCase {

    private ClusterService clusterService;
    private WatcherService watcherService;
    private WatcherLifeCycleService lifeCycleService;

    @Before
    public void prepareServices() {
        ThreadPool threadPool = mock(ThreadPool.class);
        when(threadPool.executor(anyString())).thenReturn(Runnable::run);
        clusterService = mock(ClusterService.class);
        Answer<Object> answer = new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                AckedClusterStateUpdateTask updateTask = (AckedClusterStateUpdateTask) invocationOnMock.getArguments()[1];
                updateTask.onAllNodesAcked(null);
                return null;
            }
        };
        doAnswer(answer).when(clusterService).submitStateUpdateTask(anyString(), any(ClusterStateUpdateTask.class));
        watcherService = mock(WatcherService.class);
        lifeCycleService = new WatcherLifeCycleService(Settings.EMPTY, threadPool, clusterService, watcherService);
    }

    @Test
    public void testStartAndStopCausedByClusterState() throws Exception {
        // starting... local node is master node
        DiscoveryNodes.Builder nodes = new DiscoveryNodes.Builder().masterNodeId("id1").localNodeId("id1");
        ClusterState clusterState = ClusterState.builder(new ClusterName("my-cluster"))
                .nodes(nodes).build();
        when(watcherService.state()).thenReturn(WatcherState.STOPPED);
        when(watcherService.validate(clusterState)).thenReturn(true);
        lifeCycleService.clusterChanged(new ClusterChangedEvent("any", clusterState, clusterState));
        verify(watcherService, times(1)).start(clusterState);
        verify(watcherService, never()).stop();

        // Trying to start a second time, but that should have no affect.
        when(watcherService.state()).thenReturn(WatcherState.STARTED);
        lifeCycleService.clusterChanged(new ClusterChangedEvent("any", clusterState, clusterState));
        verify(watcherService, times(1)).start(clusterState);
        verify(watcherService, never()).stop();

        // Stopping because local node is no longer master node
        nodes = new DiscoveryNodes.Builder().masterNodeId("id1").localNodeId("id2");
        ClusterState noMasterClusterState = ClusterState.builder(new ClusterName("my-cluster"))
                .nodes(nodes).build();
        lifeCycleService.clusterChanged(new ClusterChangedEvent("any", noMasterClusterState, noMasterClusterState));
        verify(watcherService, times(1)).stop();
        verify(watcherService, times(1)).start(clusterState);
    }

    @Test
    public void testStartWithStateNotRecoveredBlock() throws Exception {
        DiscoveryNodes.Builder nodes = new DiscoveryNodes.Builder().masterNodeId("id1").localNodeId("id1");
        ClusterState clusterState = ClusterState.builder(new ClusterName("my-cluster"))
                .blocks(ClusterBlocks.builder().addGlobalBlock(GatewayService.STATE_NOT_RECOVERED_BLOCK))
                .nodes(nodes).build();
        when(watcherService.state()).thenReturn(WatcherState.STOPPED);
        lifeCycleService.clusterChanged(new ClusterChangedEvent("any", clusterState, clusterState));
        verify(watcherService, never()).start(any(ClusterState.class));
    }

    @Test
    public void testManualStartStop() throws Exception {
        DiscoveryNodes.Builder nodes = new DiscoveryNodes.Builder().masterNodeId("id1").localNodeId("id1");
        ClusterState clusterState = ClusterState.builder(new ClusterName("my-cluster"))
                .nodes(nodes).build();
        when(clusterService.state()).thenReturn(clusterState);
        when(watcherService.validate(clusterState)).thenReturn(true);


        when(watcherService.state()).thenReturn(WatcherState.STOPPED);
        lifeCycleService.start();
        verify(watcherService, times(1)).start(any(ClusterState.class));
        verify(watcherService, never()).stop();

        when(watcherService.state()).thenReturn(WatcherState.STARTED);
        lifeCycleService.stop();
        verify(watcherService, times(1)).start(any(ClusterState.class));
        verify(watcherService, times(1)).stop();

        // Starting via cluster state update, we shouldn't start because we have been stopped manually.
        when(watcherService.state()).thenReturn(WatcherState.STOPPED);
        lifeCycleService.clusterChanged(new ClusterChangedEvent("any", clusterState, clusterState));
        verify(watcherService, times(1)).start(any(ClusterState.class));
        verify(watcherService, times(1)).stop();

        // we can only start, if we start manually
        lifeCycleService.start();
        verify(watcherService, times(2)).start(any(ClusterState.class));
        verify(watcherService, times(1)).stop();

        // stop watcher via cluster state update
        nodes = new DiscoveryNodes.Builder().masterNodeId("id1").localNodeId("id2");
        clusterState = ClusterState.builder(new ClusterName("my-cluster"))
                .nodes(nodes).build();
        when(watcherService.state()).thenReturn(WatcherState.STARTED);
        lifeCycleService.clusterChanged(new ClusterChangedEvent("any", clusterState, clusterState));
        verify(watcherService, times(2)).start(any(ClusterState.class));
        verify(watcherService, times(2)).stop();

        // starting watcher via cluster state update, which should work, because we manually started before
        nodes = new DiscoveryNodes.Builder().masterNodeId("id1").localNodeId("id1");
        clusterState = ClusterState.builder(new ClusterName("my-cluster"))
                .nodes(nodes).build();
        when(watcherService.validate(clusterState)).thenReturn(true);
        when(watcherService.state()).thenReturn(WatcherState.STOPPED);
        lifeCycleService.clusterChanged(new ClusterChangedEvent("any", clusterState, clusterState));
        verify(watcherService, times(3)).start(any(ClusterState.class));
        verify(watcherService, times(2)).stop();
    }

    @Test
    public void testManualStartStop_clusterStateNotValid() throws Exception {
        DiscoveryNodes.Builder nodes = new DiscoveryNodes.Builder().masterNodeId("id1").localNodeId("id1");
        ClusterState clusterState = ClusterState.builder(new ClusterName("my-cluster"))
                .nodes(nodes).build();
        when(clusterService.state()).thenReturn(clusterState);
        when(watcherService.state()).thenReturn(WatcherState.STOPPED);
        when(watcherService.validate(clusterState)).thenReturn(false);


        lifeCycleService.start();
        verify(watcherService, never()).start(any(ClusterState.class));
        verify(watcherService, never()).stop();
    }

    @Test
    public void testManualStartStop_watcherNotStopped() throws Exception {
        DiscoveryNodes.Builder nodes = new DiscoveryNodes.Builder().masterNodeId("id1").localNodeId("id1");
        ClusterState clusterState = ClusterState.builder(new ClusterName("my-cluster"))
                .nodes(nodes).build();
        when(clusterService.state()).thenReturn(clusterState);
        when(watcherService.state()).thenReturn(WatcherState.STOPPING);


        lifeCycleService.start();
        verify(watcherService, never()).validate(any(ClusterState.class));
        verify(watcherService, never()).start(any(ClusterState.class));
        verify(watcherService, never()).stop();
    }

}
