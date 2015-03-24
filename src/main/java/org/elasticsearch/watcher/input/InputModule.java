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

package org.elasticsearch.watcher.input;

import org.elasticsearch.watcher.input.search.SearchInput;
import org.elasticsearch.watcher.input.simple.SimpleInput;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.multibindings.MapBinder;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class InputModule extends AbstractModule {

    private final Map<String, Class<? extends Input.Parser>> parsers = new HashMap<>();

    public void registerInput(String type, Class<? extends Input.Parser> parserType) {
        parsers.put(type, parserType);
    }

    @Override
    protected void configure() {

        MapBinder<String, Input.Parser> parsersBinder = MapBinder.newMapBinder(binder(), String.class, Input.Parser.class);
        bind(SearchInput.Parser.class).asEagerSingleton();
        parsersBinder.addBinding(SearchInput.TYPE).to(SearchInput.Parser.class);
        bind(SimpleInput.Parser.class).asEagerSingleton();
        parsersBinder.addBinding(SimpleInput.TYPE).to(SimpleInput.Parser.class);

        for (Map.Entry<String, Class<? extends Input.Parser>> entry : parsers.entrySet()) {
            bind(entry.getValue()).asEagerSingleton();
            parsersBinder.addBinding(entry.getKey()).to(entry.getValue());
        }

        bind(InputRegistry.class).asEagerSingleton();
    }
}
