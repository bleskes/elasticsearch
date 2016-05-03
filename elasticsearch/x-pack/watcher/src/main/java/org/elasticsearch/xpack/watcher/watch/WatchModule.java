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

package org.elasticsearch.xpack.watcher.watch;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.xpack.watcher.WatcherService;
import org.elasticsearch.xpack.watcher.support.ScriptServiceProxy;

/**
 *
 */
public class WatchModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Watch.Parser.class).asEagerSingleton();
        bind(WatchLockService.class).asEagerSingleton();
        bind(WatcherService.class).asEagerSingleton();
        bind(WatchStore.class).asEagerSingleton();
        bind(ScriptServiceProxy.class).asEagerSingleton();
    }
}
