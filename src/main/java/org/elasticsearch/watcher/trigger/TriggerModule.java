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

package org.elasticsearch.watcher.trigger;

import org.elasticsearch.common.collect.ImmutableSet;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.inject.SpawnModules;
import org.elasticsearch.common.inject.multibindings.Multibinder;
import org.elasticsearch.watcher.trigger.manual.ManualTriggerEngine;
import org.elasticsearch.watcher.trigger.schedule.ScheduleModule;

import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class TriggerModule extends AbstractModule implements SpawnModules {

    private final Set<Class<? extends TriggerEngine>> engines = new HashSet<>();

    public TriggerModule() {
        registerStandardEngines();
    }

    public void registerEngine(Class<? extends TriggerEngine> engineType) {
        engines.add(engineType);
    }

    protected void registerStandardEngines() {
        registerEngine(ScheduleModule.triggerEngineType());
        registerEngine(ManualTriggerEngine.class);
    }

    @Override
    public Iterable<? extends Module> spawnModules() {
        return ImmutableSet.<Module>of(new ScheduleModule());
    }

    @Override
    protected void configure() {

        Multibinder<TriggerEngine> mbinder = Multibinder.newSetBinder(binder(), TriggerEngine.class);
        for (Class<? extends TriggerEngine> engine : engines) {
            bind(engine).asEagerSingleton();
            mbinder.addBinding().to(engine);
        }

        bind(TriggerService.class).asEagerSingleton();
    }
}
