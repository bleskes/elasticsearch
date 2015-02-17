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

package org.elasticsearch.alerts;

import org.elasticsearch.alerts.support.init.InitializingService;
import org.elasticsearch.common.collect.ImmutableList;
import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.plugins.AbstractPlugin;

import java.util.Collection;

import static org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder;

public class AlertsPlugin extends AbstractPlugin {

    public static final String NAME = "alerts";
    public static final String SCHEDULER_THREAD_POOL_NAME = "alerts_scheduler";

    private final Settings settings;

    public AlertsPlugin(Settings settings) {
        this.settings = settings;
    }

    @Override public String name() {
        return NAME;
    }

    @Override public String description() {
        return "Elasticsearch Alerts";
    }

    @Override
    public Collection<Class<? extends Module>> modules() {
        return ImmutableList.<Class<? extends Module>>of(AlertsModule.class);
    }

    @Override
    public Collection<Class<? extends LifecycleComponent>> services() {
        return ImmutableList.<Class<? extends LifecycleComponent>>of(
                // the initialization service must be first in the list
                // as other services may depend on one of the initialized
                // constructs
                InitializingService.class);
    }

    @Override
    public Settings additionalSettings() {
        int availableProcessors = EsExecutors.boundedNumberOfProcessors(settings);
        return settingsBuilder()
                .put("threadpool." + SCHEDULER_THREAD_POOL_NAME + ".type", "fixed")
                .put("threadpool." + SCHEDULER_THREAD_POOL_NAME + ".size", availableProcessors * 2)
                .put(alertThreadPoolSettings(availableProcessors, null))
                .build();
    }

    public static Settings alertThreadPoolSettings(int availableProcessors, Integer queueSize) {
        // Executing an alert involves a lot of wait time for networking (search, several index requests + optional condition logic)
        //TODO Hack to get around threadpool issue
        if (queueSize != null) {
            return settingsBuilder()
                    .put("threadpool." + NAME + ".type", "fixed")
                    .put("threadpool." + NAME + ".size", availableProcessors * 5)
                    .put("threadpool." + NAME + ".queue_size", queueSize)
                    .build();
        } else {
            return settingsBuilder()
                    .put("threadpool." + NAME + ".type", "fixed")
                    .put("threadpool." + NAME + ".size", availableProcessors * 5)
                    .build();
        }
    }

}
