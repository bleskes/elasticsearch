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

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.multibindings.MapBinder;
import org.elasticsearch.watcher.input.http.HttpInput;
import org.elasticsearch.watcher.input.http.HttpInputFactory;
import org.elasticsearch.watcher.input.none.NoneInput;
import org.elasticsearch.watcher.input.none.NoneInputFactory;
import org.elasticsearch.watcher.input.search.SearchInput;
import org.elasticsearch.watcher.input.search.SearchInputFactory;
import org.elasticsearch.watcher.input.simple.SimpleInput;
import org.elasticsearch.watcher.input.simple.SimpleInputFactory;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class InputModule extends AbstractModule {

    private final Map<String, Class<? extends InputFactory>> parsers = new HashMap<>();

    public void registerInput(String type, Class<? extends InputFactory> parserType) {
        parsers.put(type, parserType);
    }

    @Override
    protected void configure() {
        MapBinder<String, InputFactory> parsersBinder = MapBinder.newMapBinder(binder(), String.class, InputFactory.class);

        bind(SearchInputFactory.class).asEagerSingleton();
        parsersBinder.addBinding(SearchInput.TYPE).to(SearchInputFactory.class);

        bind(SimpleInputFactory.class).asEagerSingleton();
        parsersBinder.addBinding(SimpleInput.TYPE).to(SimpleInputFactory.class);

        bind(HttpInputFactory.class).asEagerSingleton();
        parsersBinder.addBinding(HttpInput.TYPE).to(HttpInputFactory.class);

        bind(NoneInputFactory.class).asEagerSingleton();
        parsersBinder.addBinding(NoneInput.TYPE).to(NoneInputFactory.class);

        for (Map.Entry<String, Class<? extends InputFactory>> entry : parsers.entrySet()) {
            bind(entry.getValue()).asEagerSingleton();
            parsersBinder.addBinding(entry.getKey()).to(entry.getValue());
        }

        bind(InputRegistry.class).asEagerSingleton();
    }
}
