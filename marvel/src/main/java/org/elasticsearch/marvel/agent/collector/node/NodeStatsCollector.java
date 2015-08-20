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

package org.elasticsearch.marvel.agent.collector.node;


import com.google.common.collect.ImmutableList;
import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.bootstrap.Bootstrap;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.routing.allocation.decider.DiskThresholdDecider;
import org.elasticsearch.common.inject.ConfigurationException;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Provider;
import org.elasticsearch.common.inject.ProvisionException;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.discovery.DiscoveryService;
import org.elasticsearch.marvel.agent.collector.AbstractCollector;
import org.elasticsearch.marvel.agent.exporter.MarvelDoc;
import org.elasticsearch.marvel.agent.settings.MarvelSettings;
import org.elasticsearch.node.service.NodeService;

import java.util.Collection;

/**
 * Collector for nodes statistics.
 * <p/>
 * This collector runs on every non-client node and collect
 * a {@link NodeStatsMarvelDoc} document for each node of the cluster.
 */
public class NodeStatsCollector extends AbstractCollector<NodeStatsCollector> {

    public static final String NAME = "node-stats-collector";
    public static final String TYPE = "marvel_node_stats";

    private final NodeService nodeService;
    private final DiscoveryService discoveryService;

    // Use a provider in order to avoid Guice circular injection
    // issues because AllocationDecider is not an interface and cannot be proxied
    private final Provider<DiskThresholdDecider> diskThresholdDeciderProvider;

    @Inject
    public NodeStatsCollector(Settings settings, ClusterService clusterService, MarvelSettings marvelSettings,
                              NodeService nodeService, DiscoveryService discoveryService,
                              Provider<DiskThresholdDecider> diskThresholdDeciderProvider) {
        super(settings, NAME, clusterService, marvelSettings);
        this.nodeService = nodeService;
        this.discoveryService = discoveryService;
        this.diskThresholdDeciderProvider = diskThresholdDeciderProvider;
    }

    @Override
    protected Collection<MarvelDoc> doCollect() throws Exception {
        ImmutableList.Builder<MarvelDoc> results = ImmutableList.builder();

        NodeStats nodeStats = nodeService.stats();

        DiskThresholdDecider diskThresholdDecider = null;
        try {
            diskThresholdDecider = diskThresholdDeciderProvider.get();
        } catch (ProvisionException | ConfigurationException e) {
            logger.warn("unable to retrieve disk threshold decider information", e);
        }

        // Here we are calling directly the DiskThresholdDecider to retrieve the high watermark value
        // It would be nicer to use a settings API like documented in #6732
        Double diskThresholdWatermarkHigh = (diskThresholdDecider != null) ? 100.0 - diskThresholdDecider.getFreeDiskThresholdHigh() : -1;
        boolean diskThresholdDeciderEnabled = (diskThresholdDecider != null) && diskThresholdDecider.isEnabled();

        results.add(new NodeStatsMarvelDoc(clusterUUID(), TYPE, System.currentTimeMillis(),
                discoveryService.localNode().id(), localNodeMaster(), nodeStats,
                Bootstrap.isMemoryLocked(), diskThresholdWatermarkHigh, diskThresholdDeciderEnabled));

        return results.build();
    }
}
