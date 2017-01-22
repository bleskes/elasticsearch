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
package org.elasticsearch.discovery;

import org.elasticsearch.cluster.ClusterChangedEvent;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.ClusterStateTaskConfig;
import org.elasticsearch.cluster.ClusterStateTaskExecutor;
import org.elasticsearch.cluster.ClusterStateTaskListener;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.routing.allocation.AllocationService;
import org.elasticsearch.cluster.service.PendingClusterTask;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.discovery.zen.ElectMasterService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A {@link Discovery} implementation that is used by {@link org.elasticsearch.tribe.TribeService}. This implementation
 * doesn't support any clustering features. Most notably {@link #startInitialJoin()} does nothing and
 * {@link #publish(ClusterChangedEvent, AckListener)} is not supported.
 */
public class NoneDiscovery extends AbstractLifecycleComponent implements Discovery {

    private final DiscoverySettings discoverySettings;
    private Supplier<ClusterState> lastAppliedClusterState;


    public NoneDiscovery(Settings settings, ClusterSettings clusterSettings, Supplier<ClusterState> lastAppliedClusterState) {
        super(settings);
        this.lastAppliedClusterState = lastAppliedClusterState;
        this.discoverySettings = new DiscoverySettings(settings, clusterSettings);
    }

    @Override
    public DiscoveryNode localNode() {
        return lastAppliedClusterState.get().nodes().getLocalNode();
    }

    @Override
    public String nodeDescription() {
        final ClusterState state = lastAppliedClusterState.get();
        return state.getClusterName().value() + "/" + state.nodes().getLocalNodeId();
    }

    @Override
    public void setAllocationService(AllocationService allocationService) {

    }

    @Override
    public void publish(ClusterChangedEvent clusterChangedEvent, AckListener ackListener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DiscoveryStats stats() {
        return null;
    }

    @Override
    public DiscoverySettings getDiscoverySettings() {
        return discoverySettings;
    }

    @Override
    public void startInitialJoin() {

    }

    @Override
    public int getMinimumMasterNodes() {
        return ElectMasterService.DISCOVERY_ZEN_MINIMUM_MASTER_NODES_SETTING.get(settings);
    }

    @Override
    public synchronized <T> void submitStateUpdateTasks(String source, Map<T, ClusterStateTaskListener> tasks, ClusterStateTaskConfig config, ClusterStateTaskExecutor<T> executor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<PendingClusterTask> pendingTasks() {
        return Collections.emptyList();
    }

    @Override
    public int numberOfPendingTasks() {
        return 0;
    }

    @Override
    public TimeValue getMaxTaskWaitTime() {
        return TimeValue.ZERO;
    }

    @Override
    protected void doStart() {

    }

    @Override
    protected void doStop() {

    }

    @Override
    protected void doClose() {

    }
}
