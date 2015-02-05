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


import org.elasticsearch.alerts.actions.ActionModule;
import org.elasticsearch.alerts.client.AlertsClientModule;
import org.elasticsearch.alerts.history.HistoryService;
import org.elasticsearch.alerts.payload.PayloadModule;
import org.elasticsearch.alerts.rest.AlertsRestModule;
import org.elasticsearch.alerts.scheduler.SchedulerModule;
import org.elasticsearch.alerts.support.TemplateUtils;
import org.elasticsearch.alerts.support.init.InitializingModule;
import org.elasticsearch.alerts.transport.AlertsTransportModule;
import org.elasticsearch.alerts.trigger.TriggerModule;
import org.elasticsearch.common.collect.ImmutableList;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.inject.SpawnModules;


public class AlertsModule extends AbstractModule implements SpawnModules {

    @Override
    public Iterable<? extends Module> spawnModules() {
        return ImmutableList.of(
                new InitializingModule(),
                new AlertsClientModule(),
                new PayloadModule(),
                new AlertsRestModule(),
                new SchedulerModule(),
                new AlertsTransportModule(),
                new TriggerModule(),
                new ActionModule());
    }

    @Override
    protected void configure() {

        bind(Alert.Parser.class).asEagerSingleton();
        bind(AlertsService.class).asEagerSingleton();
        bind(AlertsStore.class).asEagerSingleton();
        bind(TemplateUtils.class).asEagerSingleton();
        bind(HistoryService.class).asEagerSingleton();
        bind(ConfigurationService.class).asEagerSingleton();

    }

}
