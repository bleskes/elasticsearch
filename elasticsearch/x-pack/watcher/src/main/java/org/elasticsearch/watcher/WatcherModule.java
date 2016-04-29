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

package org.elasticsearch.watcher;


import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.util.Providers;
import org.elasticsearch.watcher.support.WatcherIndexTemplateRegistry;
import org.elasticsearch.watcher.support.validation.WatcherSettingsValidation;
import org.elasticsearch.xpack.XPackPlugin;


public class WatcherModule extends AbstractModule {

    private final boolean enabled;
    private final boolean transportClientMode;

    public WatcherModule(boolean enabled, boolean transportClientMode) {
        this.enabled = enabled;
        this.transportClientMode = transportClientMode;
    }

    @Override
    protected void configure() {
        XPackPlugin.bindFeatureSet(binder(), WatcherFeatureSet.class);

        if (enabled == false || transportClientMode) {
            bind(WatcherLicensee.class).toProvider(Providers.of(null));
            return;
        }

        bind(WatcherLicensee.class).asEagerSingleton();
        bind(WatcherLifeCycleService.class).asEagerSingleton();
        bind(WatcherSettingsValidation.class).asEagerSingleton();
        bind(WatcherIndexTemplateRegistry.class).asEagerSingleton();
    }
}
