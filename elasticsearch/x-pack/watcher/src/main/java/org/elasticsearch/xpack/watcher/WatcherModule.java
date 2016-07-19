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

package org.elasticsearch.xpack.watcher;


import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.util.Providers;
import org.elasticsearch.xpack.XPackPlugin;
import org.elasticsearch.xpack.watcher.support.WatcherIndexTemplateRegistry;


public class WatcherModule extends AbstractModule {

    private final boolean enabled;
    private final boolean transportClientMode;

    public WatcherModule(boolean enabled, boolean transportClientMode) {
        this.enabled = enabled;
        this.transportClientMode = transportClientMode;
    }

    @Override
    protected void configure() {
        if (transportClientMode) {
            return;
        }

        if (enabled == false) {
            // watcher service must be null, so that the watcher feature set can be instantiated even if watcher is not enabled
            bind(WatcherService.class).toProvider(Providers.of(null));
        } else {
            bind(WatcherLifeCycleService.class).asEagerSingleton();
            bind(WatcherIndexTemplateRegistry.class).asEagerSingleton();
        }

        XPackPlugin.bindFeatureSet(binder(), WatcherFeatureSet.class);
    }
}
