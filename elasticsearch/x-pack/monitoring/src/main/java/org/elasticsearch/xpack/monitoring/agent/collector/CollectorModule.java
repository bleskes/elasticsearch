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

package org.elasticsearch.xpack.monitoring.agent.collector;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.multibindings.Multibinder;
import org.elasticsearch.xpack.monitoring.agent.collector.cluster.ClusterStateCollector;
import org.elasticsearch.xpack.monitoring.agent.collector.cluster.ClusterStatsCollector;
import org.elasticsearch.xpack.monitoring.agent.collector.indices.IndexRecoveryCollector;
import org.elasticsearch.xpack.monitoring.agent.collector.indices.IndexStatsCollector;
import org.elasticsearch.xpack.monitoring.agent.collector.indices.IndicesStatsCollector;
import org.elasticsearch.xpack.monitoring.agent.collector.node.NodeStatsCollector;
import org.elasticsearch.xpack.monitoring.agent.collector.shards.ShardsCollector;

import java.util.HashSet;
import java.util.Set;

public class CollectorModule extends AbstractModule {

    private final Set<Class<? extends Collector>> collectors = new HashSet<>();

    public CollectorModule() {
        // Registers default collectors
        registerCollector(IndicesStatsCollector.class);
        registerCollector(IndexStatsCollector.class);
        registerCollector(ClusterStatsCollector.class);
        registerCollector(ClusterStateCollector.class);
        registerCollector(ShardsCollector.class);
        registerCollector(NodeStatsCollector.class);
        registerCollector(IndexRecoveryCollector.class);
    }

    @Override
    protected void configure() {
        Multibinder<Collector> binder = Multibinder.newSetBinder(binder(), Collector.class);
        for (Class<? extends Collector> collector : collectors) {
            bind(collector).asEagerSingleton();
            binder.addBinding().to(collector);
        }
    }

    public void registerCollector(Class<? extends Collector> collector) {
        collectors.add(collector);
    }
}