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
import org.elasticsearch.common.inject.multibindings.MapBinder;
import org.elasticsearch.marvel.agent.collector.cluster.ClusterInfoCollector;
import org.elasticsearch.marvel.agent.collector.cluster.ClusterStateCollector;
import org.elasticsearch.marvel.agent.collector.cluster.ClusterStatsCollector;
import org.elasticsearch.marvel.agent.collector.indices.IndexRecoveryCollector;
import org.elasticsearch.marvel.agent.collector.indices.IndexStatsCollector;
import org.elasticsearch.marvel.agent.collector.indices.IndicesStatsCollector;
import org.elasticsearch.marvel.agent.collector.node.NodeStatsCollector;
import org.elasticsearch.marvel.agent.renderer.cluster.ClusterInfoRenderer;
import org.elasticsearch.marvel.agent.renderer.cluster.ClusterStateRenderer;
import org.elasticsearch.marvel.agent.renderer.cluster.ClusterStatsRenderer;
import org.elasticsearch.marvel.agent.renderer.indices.IndexRecoveryRenderer;
import org.elasticsearch.marvel.agent.renderer.indices.IndexStatsRenderer;
import org.elasticsearch.marvel.agent.renderer.indices.IndicesStatsRenderer;
import org.elasticsearch.marvel.agent.renderer.node.NodeStatsRenderer;

import java.util.HashMap;
import java.util.Map;

public class RendererModule extends AbstractModule {

    private Map<String, Class<? extends Renderer>> renderers = new HashMap<>();

    public void registerRenderer(String payloadType, Class<? extends Renderer> renderer) {
        renderers.put(payloadType, renderer);
    }

    @Override
    protected void configure() {
        MapBinder<String, Renderer> mbinder = MapBinder.newMapBinder(binder(), String.class, Renderer.class);

        // Bind default renderers
        bind(ClusterInfoRenderer.class).asEagerSingleton();
        mbinder.addBinding(ClusterInfoCollector.TYPE).to(ClusterInfoRenderer.class);

        bind(IndicesStatsRenderer.class).asEagerSingleton();
        mbinder.addBinding(IndicesStatsCollector.TYPE).to(IndicesStatsRenderer.class);

        bind(IndexStatsRenderer.class).asEagerSingleton();
        mbinder.addBinding(IndexStatsCollector.TYPE).to(IndexStatsRenderer.class);

        bind(ClusterStatsRenderer.class).asEagerSingleton();
        mbinder.addBinding(ClusterStatsCollector.TYPE).to(ClusterStatsRenderer.class);

        bind(ClusterStateRenderer.class).asEagerSingleton();
        mbinder.addBinding(ClusterStateCollector.TYPE).to(ClusterStateRenderer.class);

        bind(NodeStatsRenderer.class).asEagerSingleton();
        mbinder.addBinding(NodeStatsCollector.TYPE).to(NodeStatsRenderer.class);

        bind(IndexRecoveryRenderer.class).asEagerSingleton();
        mbinder.addBinding(IndexRecoveryCollector.TYPE).to(IndexRecoveryRenderer.class);

        for (Map.Entry<String, Class<? extends Renderer>> entry : renderers.entrySet()) {
            bind(entry.getValue()).asEagerSingleton();
            mbinder.addBinding(entry.getKey()).to(entry.getValue());
        }
    }
}
