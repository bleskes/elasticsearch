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

package org.elasticsearch.watcher.condition;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.multibindings.MapBinder;
import org.elasticsearch.watcher.condition.always.AlwaysCondition;
import org.elasticsearch.watcher.condition.always.AlwaysConditionFactory;
import org.elasticsearch.watcher.condition.compare.CompareCondition;
import org.elasticsearch.watcher.condition.compare.CompareConditionFactory;
import org.elasticsearch.watcher.condition.never.NeverCondition;
import org.elasticsearch.watcher.condition.never.NeverConditionFactory;
import org.elasticsearch.watcher.condition.script.ScriptCondition;
import org.elasticsearch.watcher.condition.script.ScriptConditionFactory;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class ConditionModule extends AbstractModule {

    private final Map<String, Class<? extends ConditionFactory>> factories = new HashMap<>();

    public void registerCondition(String type, Class<? extends ConditionFactory> factoryType) {
        factories.put(type, factoryType);
    }

    @Override
    protected void configure() {

        MapBinder<String, ConditionFactory> factoriesBinder = MapBinder.newMapBinder(binder(), String.class, ConditionFactory.class);

        bind(ScriptConditionFactory.class).asEagerSingleton();
        factoriesBinder.addBinding(ScriptCondition.TYPE).to(ScriptConditionFactory.class);

        bind(NeverConditionFactory.class).asEagerSingleton();
        factoriesBinder.addBinding(NeverCondition.TYPE).to(NeverConditionFactory.class);

        bind(AlwaysConditionFactory.class).asEagerSingleton();
        factoriesBinder.addBinding(AlwaysCondition.TYPE).to(AlwaysConditionFactory.class);

        bind(CompareConditionFactory.class).asEagerSingleton();
        factoriesBinder.addBinding(CompareCondition.TYPE).to(CompareConditionFactory.class);

        for (Map.Entry<String, Class<? extends ConditionFactory>> entry : factories.entrySet()) {
            bind(entry.getValue()).asEagerSingleton();
            factoriesBinder.addBinding(entry.getKey()).to(entry.getValue());
        }

        bind(ConditionRegistry.class).asEagerSingleton();
    }
}
