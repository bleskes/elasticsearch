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

package org.elasticsearch.alerts.condition;

import org.elasticsearch.alerts.condition.search.ScriptSearchCondition;
import org.elasticsearch.alerts.condition.simple.SimpleCondition;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.multibindings.MapBinder;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class ConditionModule extends AbstractModule {

    private final Map<String, Class<? extends Condition.Parser>> parsers = new HashMap<>();

    public void registerCondition(String type, Class<? extends Condition.Parser> parserType) {
        parsers.put(type, parserType);
    }

    @Override
    protected void configure() {

        MapBinder<String, Condition.Parser> parsersBinder = MapBinder.newMapBinder(binder(), String.class, Condition.Parser.class);
        bind(ScriptSearchCondition.Parser.class).asEagerSingleton();
        parsersBinder.addBinding(ScriptSearchCondition.TYPE).to(ScriptSearchCondition.Parser.class);
        bind(SimpleCondition.Parser.class).asEagerSingleton();
        parsersBinder.addBinding(SimpleCondition.TYPE).to(SimpleCondition.Parser.class);

        for (Map.Entry<String, Class<? extends Condition.Parser>> entry : parsers.entrySet()) {
            bind(entry.getValue()).asEagerSingleton();
            parsersBinder.addBinding(entry.getKey()).to(entry.getValue());
        }

        bind(ConditionRegistry.class).asEagerSingleton();
    }
}
