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

package org.elasticsearch.xpack.monitoring;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.util.Providers;
import org.elasticsearch.xpack.XPackPlugin;
import org.elasticsearch.xpack.monitoring.agent.AgentService;
import org.elasticsearch.xpack.monitoring.cleaner.CleanerService;

public class MonitoringModule extends AbstractModule {

    private final boolean enabled;
    private final boolean transportClientMode;

    public MonitoringModule(boolean enabled, boolean transportClientMode) {
        this.enabled = enabled;
        this.transportClientMode = transportClientMode;
    }

    @Override
    protected void configure() {
        XPackPlugin.bindFeatureSet(binder(), MonitoringFeatureSet.class);

        if (enabled && transportClientMode == false) {
            bind(MonitoringLicensee.class).asEagerSingleton();
            bind(MonitoringSettings.class).asEagerSingleton();
            bind(AgentService.class).asEagerSingleton();
            bind(CleanerService.class).asEagerSingleton();
        } else {
            bind(MonitoringLicensee.class).toProvider(Providers.of(null));
        }
    }
}
