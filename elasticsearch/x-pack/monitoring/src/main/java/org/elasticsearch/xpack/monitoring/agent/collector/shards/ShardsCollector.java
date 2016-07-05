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

package org.elasticsearch.xpack.monitoring.agent.collector.shards;

import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.routing.RoutingTable;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.regex.Regex;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.monitoring.MonitoringLicensee;
import org.elasticsearch.xpack.monitoring.MonitoringSettings;
import org.elasticsearch.xpack.monitoring.agent.collector.AbstractCollector;
import org.elasticsearch.xpack.monitoring.agent.exporter.MonitoringDoc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Collector for shards.
 * <p>
 * This collector runs on the master node only and collects the {@link ShardMonitoringDoc} documents
 * for every index shard.
 */
public class ShardsCollector extends AbstractCollector {

    public static final String NAME = "shards-collector";

    @Inject
    public ShardsCollector(Settings settings, ClusterService clusterService,
                           MonitoringSettings monitoringSettings, MonitoringLicensee licensee) {
        super(settings, NAME, clusterService, monitoringSettings, licensee);
    }

    @Override
    protected boolean shouldCollect() {
        return super.shouldCollect() && isLocalNodeMaster();
    }

    @Override
    protected Collection<MonitoringDoc> doCollect() throws Exception {
        List<MonitoringDoc> results = new ArrayList<>(1);

        ClusterState clusterState = clusterService.state();
        if (clusterState != null) {
            RoutingTable routingTable = clusterState.routingTable();
            if (routingTable != null) {
                List<ShardRouting> shards = routingTable.allShards();
                if (shards != null) {
                    String clusterUUID = clusterUUID();
                    String stateUUID = clusterState.stateUUID();
                    long timestamp = System.currentTimeMillis();

                    for (ShardRouting shard : shards) {
                        if (match(shard.getIndexName())) {
                            ShardMonitoringDoc shardDoc = new ShardMonitoringDoc(monitoringId(), monitoringVersion());
                            shardDoc.setClusterUUID(clusterUUID);
                            shardDoc.setTimestamp(timestamp);
                            if (shard.assignedToNode()) {
                                // If the shard is assigned to a node, the shard monitoring document
                                // refers to this node
                                shardDoc.setSourceNode(clusterState.getNodes().get(shard.currentNodeId()));
                            }
                            shardDoc.setShardRouting(shard);
                            shardDoc.setClusterStateUUID(stateUUID);
                            results.add(shardDoc);
                        }
                    }
                }
            }
        }

        return Collections.unmodifiableCollection(results);
    }

    private boolean match(String indexName) {
        String[] indices = monitoringSettings.indices();
        return IndexNameExpressionResolver
                .isAllIndices(Arrays.asList(monitoringSettings.indices())) || Regex.simpleMatch(indices, indexName);
    }
}
