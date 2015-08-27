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

package org.elasticsearch.watcher.shield;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.util.Providers;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.authz.Privilege;

/**
 *
 */
public class WatcherShieldModule extends AbstractModule {

    private final ESLogger logger;

    private final boolean enabled;

    private final WatcherUserHolder userHolder;

    public WatcherShieldModule(Settings settings) {
        this.logger = Loggers.getLogger(WatcherShieldModule.class, settings);
        this.enabled = ShieldIntegration.enabled(settings);
        if (enabled) {
            userHolder = new WatcherUserHolder();
            registerClusterPrivilege("manage_watcher", "cluster:admin/watcher/*", "cluster:monitor/watcher/*");
            registerClusterPrivilege("monitor_watcher", "cluster:monitor/watcher/*");
        } else {
            userHolder = null;
        }
    }

    void registerClusterPrivilege(String name, String... patterns) {
        try {
            Privilege.Cluster.addCustom(name, patterns);
        } catch (Exception se) {
            logger.warn("could not register cluster privilege [{}]", name);

            // we need to prevent bubbling the shield exception here for the tests. In the tests
            // we create multiple nodes in the same jvm and since the custom cluster is a static binding
            // multiple nodes will try to add the same privileges multiple times.
        }
    }

    @Override
    protected void configure() {
        bind(ShieldIntegration.class).asEagerSingleton();
        bind(WatcherUserHolder.class).toProvider(Providers.of(userHolder));
        if (enabled) {
            bind(WatcherSettingsFilter.Shield.class).asEagerSingleton();
            bind(WatcherSettingsFilter.class).to(WatcherSettingsFilter.Shield.class);
        } else {
            bind(WatcherSettingsFilter.class).toInstance(WatcherSettingsFilter.Noop.INSTANCE);
        }
    }
}
