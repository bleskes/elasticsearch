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

package org.elasticsearch.xpack.monitoring.collector.shards;

import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.routing.RoutingTable;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.regex.Regex;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.xpack.monitoring.MonitoringSettings;
import org.elasticsearch.xpack.monitoring.collector.Collector;
import org.elasticsearch.xpack.monitoring.exporter.MonitoringDoc;

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
public class ShardsCollector extends Collector {

    public ShardsCollector(Settings settings, ClusterService clusterService,
                           MonitoringSettings monitoringSettings, XPackLicenseState licenseState) {
        super(settings, "shards", clusterService, monitoringSettings, licenseState);
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
                            DiscoveryNode node = null;
                            if (shard.assignedToNode()) {
                                // If the shard is assigned to a node, the shard monitoring document
                                // refers to this node
                                node = clusterState.getNodes().get(shard.currentNodeId());
                            }
                            results.add(new ShardMonitoringDoc(monitoringId(), monitoringVersion(),
                                    clusterUUID, timestamp, node, shard, stateUUID));
                        }
                    }
                }
            }
        }
        return Collections.unmodifiableCollection(results);
    }

    private boolean match(String indexName) {
        String[] indices = monitoringSettings.indices();
        return IndexNameExpressionResolver.isAllIndices(Arrays.asList(monitoringSettings.indices()))
                || Regex.simpleMatch(indices, indexName);
    }
}
