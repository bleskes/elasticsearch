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

package org.elasticsearch.xpack.common.init;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.multibindings.Multibinder;

import java.util.HashSet;
import java.util.Set;

/**
 * A module to lazy initialize objects and avoid circular dependency injection issues.
 *
 * Objects that use the {@link org.elasticsearch.client.ElasticsearchClient} and that are also injected in transport actions provoke
 * a circular dependency injection issues with Guice. Using proxies with lazy initialization is a way to solve this issue.
 *
 * The proxies are initialized by {@link LazyInitializationService}.
 */
public class LazyInitializationModule extends AbstractModule {

    private final Set<Class<? extends LazyInitializable>> initializables = new HashSet<>();

    @Override
    protected void configure() {
        Multibinder<LazyInitializable> mbinder = Multibinder.newSetBinder(binder(), LazyInitializable.class);
        for (Class<? extends LazyInitializable> initializable : initializables) {
            bind(initializable).asEagerSingleton();
            mbinder.addBinding().to(initializable);
        }
        bind(LazyInitializationService.class).asEagerSingleton();
    }

    public void registerLazyInitializable(Class<? extends LazyInitializable> lazyTypeClass) {
        initializables.add(lazyTypeClass);
    }
}
