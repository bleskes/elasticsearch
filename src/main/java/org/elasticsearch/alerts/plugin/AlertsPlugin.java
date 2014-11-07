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

package org.elasticsearch.alerts.plugin;

import org.elasticsearch.alerts.AlertingModule;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.AbstractPlugin;

import java.util.Collection;

import static org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder;

public class AlertsPlugin extends AbstractPlugin {

    public static final String ALERT_THREAD_POOL_NAME = "alerts";
    public static final String SCHEDULER_THREAD_POOL_NAME = "alerts";

    @Override public String name() {
        return ALERT_THREAD_POOL_NAME;
    }

    @Override public String description() {
        return "Elasticsearch Alerts";
    }

    @Override
    public Collection<Class<? extends Module>> modules() {
        Collection<Class<? extends Module>> modules = Lists.newArrayList();
        modules.add(AlertingModule.class);
        return modules;
    }

    @Override
    public Settings additionalSettings() {
        return settingsBuilder()
                .put("threadpool." + ALERT_THREAD_POOL_NAME + ".type", "fixed")
                .put("threadpool." + ALERT_THREAD_POOL_NAME + ".size", 32) // Executing an alert involves a lot of wait time for networking (search, several index requests + optional trigger logic)
                .put("threadpool." + SCHEDULER_THREAD_POOL_NAME + ".type", "cached")
                .build();
    }


}
