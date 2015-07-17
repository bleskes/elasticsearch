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
import org.elasticsearch.common.inject.multibindings.Multibinder;

import java.util.HashSet;
import java.util.Set;

public class ExporterModule extends AbstractModule {

    private final Set<Class<? extends Exporter>> exporters = new HashSet<>();

    public ExporterModule() {
        // Registers default exporter
        registerExporter(HttpESExporter.class);
    }

    @Override
    protected void configure() {
        Multibinder<Exporter> binder = Multibinder.newSetBinder(binder(), Exporter.class);
        for (Class<? extends Exporter> exporter : exporters) {
            bind(exporter).asEagerSingleton();
            binder.addBinding().to(exporter);
        }
    }

    public void registerExporter(Class<? extends Exporter> exporter) {
        exporters.add(exporter);
    }
}
