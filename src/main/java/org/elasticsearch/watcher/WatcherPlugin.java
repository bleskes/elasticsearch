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

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.watcher.actions.email.service.InternalEmailService;
import org.elasticsearch.watcher.license.LicenseService;
import org.elasticsearch.watcher.support.init.InitializingService;
import org.elasticsearch.common.collect.ImmutableList;
import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.plugins.AbstractPlugin;

import java.util.Collection;

import static org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder;

public class WatcherPlugin extends AbstractPlugin {

    public static final String NAME = "watcher";
    public static final String SCHEDULER_THREAD_POOL_NAME = "watcher_scheduler";

    private final Settings settings;
    private final boolean transportClient;

    public WatcherPlugin(Settings settings) {
        this.settings = settings;
        transportClient = "transport".equals(settings.get(Client.CLIENT_TYPE_SETTING));
    }

    @Override public String name() {
        return NAME;
    }

    @Override public String description() {
        return "Elasticsearch Watcher";
    }

    @Override
    public Collection<Class<? extends Module>> modules() {
        return transportClient ?
                ImmutableList.<Class<? extends Module>>of(TransportClientWatcherModule.class) :
                ImmutableList.<Class<? extends Module>>of(WatcherModule.class);
    }

    @Override
    public Collection<Class<? extends LifecycleComponent>> services() {
        if (transportClient) {
            return ImmutableList.of();
        }
        return ImmutableList.<Class<? extends LifecycleComponent>>of(
                // the initialization service must be first in the list
                // as other services may depend on one of the initialized
                // constructs
                InitializingService.class,
                LicenseService.class,
                InternalEmailService.class);
    }

    @Override
    public Settings additionalSettings() {
        if (transportClient) {
            return ImmutableSettings.EMPTY;
        }
        int availableProcessors = EsExecutors.boundedNumberOfProcessors(settings);
        return settingsBuilder()
                .put("threadpool." + SCHEDULER_THREAD_POOL_NAME + ".type", "fixed")
                .put("threadpool." + SCHEDULER_THREAD_POOL_NAME + ".size", availableProcessors * 2)
                .put("threadpool." + SCHEDULER_THREAD_POOL_NAME + ".queue_size", 1000)
                .put("threadpool." + NAME + ".type", "fixed")
                .put("threadpool." + NAME + ".size", availableProcessors * 5)
                .put("threadpool." + NAME + ".queue_size", 1000)
                .build();
    }

}
