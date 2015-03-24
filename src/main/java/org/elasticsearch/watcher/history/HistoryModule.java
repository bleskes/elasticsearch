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

package org.elasticsearch.watcher.history;

import org.elasticsearch.common.inject.AbstractModule;

/**
 */
public class HistoryModule extends AbstractModule {

    private final Class<? extends WatchExecutor> executorClass;

    public HistoryModule() {
        this(InternalWatchExecutor.class);
    }

    protected HistoryModule(Class<? extends WatchExecutor> executorClass) {
        this.executorClass = executorClass;
    }

    @Override
    protected void configure() {
        bind(WatchRecord.Parser.class).asEagerSingleton();
        bind(HistoryStore.class).asEagerSingleton();
        bind(HistoryService.class).asEagerSingleton();
        bind(executorClass).asEagerSingleton();
        bind(WatchExecutor.class).to(executorClass);
    }
}
