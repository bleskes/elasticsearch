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
import org.elasticsearch.marvel.agent.collector.indices.IndexStatsCollector;
import org.elasticsearch.marvel.agent.renderer.indices.IndexStatsRenderer;

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
        bind(IndexStatsRenderer.class).asEagerSingleton();
        mbinder.addBinding(IndexStatsCollector.TYPE).to(IndexStatsRenderer.class);

        for (Map.Entry<String, Class<? extends Renderer>> entry : renderers.entrySet()) {
            bind(entry.getValue()).asEagerSingleton();
            mbinder.addBinding(entry.getKey()).to(entry.getValue());
        }
    }
}
