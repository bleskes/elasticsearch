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

package org.elasticsearch.xpack.watcher.execution;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.xpack.watcher.trigger.TriggerEngine;

/**
 */
public class ExecutionModule extends AbstractModule {

    private final Class<? extends WatchExecutor> executorClass;
    private final Class<? extends TriggerEngine.Listener> triggerEngineListenerClass;

    public ExecutionModule() {
        this(InternalWatchExecutor.class, AsyncTriggerListener.class);
    }

    protected ExecutionModule(Class<? extends WatchExecutor> executorClass,
                              Class<? extends TriggerEngine.Listener> triggerEngineListenerClass) {
        this.executorClass = executorClass;
        this.triggerEngineListenerClass = triggerEngineListenerClass;
    }

    @Override
    protected void configure() {
        bind(TriggeredWatch.Parser.class).asEagerSingleton();
        bind(TriggeredWatchStore.class).asEagerSingleton();
        bind(ExecutionService.class).asEagerSingleton();
        bind(executorClass).asEagerSingleton();
        bind(triggerEngineListenerClass).asEagerSingleton();
        bind(WatchExecutor.class).to(executorClass);
    }
}
