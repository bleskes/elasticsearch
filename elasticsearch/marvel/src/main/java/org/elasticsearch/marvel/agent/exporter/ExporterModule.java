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

package org.elasticsearch.marvel.agent.exporter;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.multibindings.MapBinder;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.marvel.agent.exporter.http.HttpExporter;
import org.elasticsearch.marvel.agent.exporter.local.LocalExporter;

import java.util.HashMap;
import java.util.Map;

public class ExporterModule extends AbstractModule {

    private final Map<String, Class<? extends Exporter.Factory<? extends Exporter>>> exporterFactories = new HashMap<>();

    private final Settings settings;

    public ExporterModule(Settings settings) {
        this.settings = settings;
        registerExporter(HttpExporter.TYPE, HttpExporter.Factory.class);
        registerExporter(LocalExporter.TYPE, LocalExporter.Factory.class);
    }

    @Override
    protected void configure() {
        bind(Exporters.class).asEagerSingleton();
        MapBinder<String, Exporter.Factory> factoryBinder = MapBinder.newMapBinder(binder(), String.class, Exporter.Factory.class);
        for (Map.Entry<String, Class<? extends Exporter.Factory<? extends Exporter>>> entry : exporterFactories.entrySet()) {
            bind(entry.getValue()).asEagerSingleton();
            factoryBinder.addBinding(entry.getKey()).to(entry.getValue());
        }

    }

    public void registerExporter(String type, Class<? extends Exporter.Factory<? extends Exporter>> factory) {
        exporterFactories.put(type, factory);
    }
}
