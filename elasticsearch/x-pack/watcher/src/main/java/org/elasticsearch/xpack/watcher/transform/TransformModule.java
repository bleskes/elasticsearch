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

package org.elasticsearch.xpack.watcher.transform;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.multibindings.MapBinder;
import org.elasticsearch.xpack.watcher.transform.chain.ChainTransform;
import org.elasticsearch.xpack.watcher.transform.chain.ChainTransformFactory;
import org.elasticsearch.xpack.watcher.transform.script.ScriptTransform;
import org.elasticsearch.xpack.watcher.transform.script.ScriptTransformFactory;
import org.elasticsearch.xpack.watcher.transform.search.SearchTransform;
import org.elasticsearch.xpack.watcher.transform.search.SearchTransformFactory;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class TransformModule extends AbstractModule {

    private Map<String, Class<? extends TransformFactory>> factories = new HashMap<>();

    public void registerTransform(String payloadType, Class<? extends TransformFactory> parserType) {
        factories.put(payloadType, parserType);
    }

    @Override
    protected void configure() {
        MapBinder<String, TransformFactory> mbinder = MapBinder.newMapBinder(binder(), String.class, TransformFactory.class);

        bind(SearchTransformFactory.class).asEagerSingleton();
        mbinder.addBinding(SearchTransform.TYPE).to(SearchTransformFactory.class);

        bind(ScriptTransformFactory.class).asEagerSingleton();
        mbinder.addBinding(ScriptTransform.TYPE).to(ScriptTransformFactory.class);

        // no bind() needed, done using the LazyInitializationModule
        mbinder.addBinding(ChainTransform.TYPE).to(ChainTransformFactory.class);

        for (Map.Entry<String, Class<? extends TransformFactory>> entry : factories.entrySet()) {
            bind(entry.getValue()).asEagerSingleton();
            mbinder.addBinding(entry.getKey()).to(entry.getValue());
        }
    }
}
