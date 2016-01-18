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

package org.elasticsearch.marvel.agent.renderer;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.marvel.agent.collector.cluster.ClusterInfoMarvelDoc;
import org.elasticsearch.marvel.agent.collector.cluster.ClusterStateMarvelDoc;
import org.elasticsearch.marvel.agent.collector.cluster.ClusterStateNodeMarvelDoc;
import org.elasticsearch.marvel.agent.collector.cluster.ClusterStatsMarvelDoc;
import org.elasticsearch.marvel.agent.collector.cluster.DiscoveryNodeMarvelDoc;
import org.elasticsearch.marvel.agent.collector.indices.IndexRecoveryMarvelDoc;
import org.elasticsearch.marvel.agent.collector.indices.IndexStatsMarvelDoc;
import org.elasticsearch.marvel.agent.collector.indices.IndicesStatsMarvelDoc;
import org.elasticsearch.marvel.agent.collector.node.NodeStatsMarvelDoc;
import org.elasticsearch.marvel.agent.collector.shards.ShardMarvelDoc;
import org.elasticsearch.marvel.agent.exporter.MarvelDoc;
import org.elasticsearch.marvel.agent.renderer.cluster.ClusterInfoRenderer;
import org.elasticsearch.marvel.agent.renderer.cluster.ClusterStateNodeRenderer;
import org.elasticsearch.marvel.agent.renderer.cluster.ClusterStateRenderer;
import org.elasticsearch.marvel.agent.renderer.cluster.ClusterStatsRenderer;
import org.elasticsearch.marvel.agent.renderer.cluster.DiscoveryNodeRenderer;
import org.elasticsearch.marvel.agent.renderer.indices.IndexRecoveryRenderer;
import org.elasticsearch.marvel.agent.renderer.indices.IndexStatsRenderer;
import org.elasticsearch.marvel.agent.renderer.indices.IndicesStatsRenderer;
import org.elasticsearch.marvel.agent.renderer.node.NodeStatsRenderer;
import org.elasticsearch.marvel.agent.renderer.shards.ShardsRenderer;

import java.util.HashMap;
import java.util.Map;

public class RendererModule extends AbstractModule {

    protected void configure() {
        Map<Class<? extends MarvelDoc>, Renderer> renderers = new HashMap<>();
        renderers.put(IndicesStatsMarvelDoc.class, new IndicesStatsRenderer());
        renderers.put(IndexStatsMarvelDoc.class, new IndexStatsRenderer());
        renderers.put(ClusterInfoMarvelDoc.class, new ClusterInfoRenderer());
        renderers.put(ClusterStatsMarvelDoc.class, new ClusterStatsRenderer());
        renderers.put(ClusterStateMarvelDoc.class, new ClusterStateRenderer());
        renderers.put(ClusterStateNodeMarvelDoc.class, new ClusterStateNodeRenderer());
        renderers.put(DiscoveryNodeMarvelDoc.class, new DiscoveryNodeRenderer());
        renderers.put(ShardMarvelDoc.class, new ShardsRenderer());
        renderers.put(NodeStatsMarvelDoc.class, new NodeStatsRenderer());
        renderers.put(IndexRecoveryMarvelDoc.class, new IndexRecoveryRenderer());

        RendererRegistry registry = new RendererRegistry(renderers);
        bind(RendererRegistry.class).toInstance(registry);
    }
}
