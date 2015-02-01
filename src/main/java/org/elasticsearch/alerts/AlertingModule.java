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


import org.elasticsearch.alerts.actions.AlertActionRegistry;
import org.elasticsearch.alerts.actions.AlertActionService;
import org.elasticsearch.alerts.client.AlertsClientModule;
import org.elasticsearch.alerts.rest.*;
import org.elasticsearch.alerts.scheduler.AlertScheduler;
import org.elasticsearch.alerts.support.init.InitializingModule;
import org.elasticsearch.alerts.transport.AlertsTransportModule;
import org.elasticsearch.alerts.triggers.TriggerService;
import org.elasticsearch.common.collect.ImmutableList;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.inject.SpawnModules;


public class AlertingModule extends AbstractModule implements SpawnModules {

    @Override
    public Iterable<? extends Module> spawnModules() {
        return ImmutableList.of(
                new InitializingModule(),
                new AlertsTransportModule(),
                new AlertsClientModule());
    }

    @Override
    protected void configure() {

        // Core components
        bind(TemplateHelper.class).asEagerSingleton();
        bind(AlertsStore.class).asEagerSingleton();
        bind(AlertService.class).asEagerSingleton();
        bind(AlertActionService.class).asEagerSingleton();
        bind(TriggerService.class).asEagerSingleton();
        bind(AlertScheduler.class).asEagerSingleton();
        bind(AlertActionRegistry.class).asEagerSingleton();
        bind(ConfigurationService.class).asEagerSingleton();

        // Rest layer
        bind(RestPutAlertAction.class).asEagerSingleton();
        bind(RestDeleteAlertAction.class).asEagerSingleton();
        bind(RestAlertsStatsAction.class).asEagerSingleton();
        bind(RestGetAlertAction.class).asEagerSingleton();
        bind(RestAlertServiceAction.class).asEagerSingleton();
        bind(RestConfigAlertAction.class).asEagerSingleton();
        bind(RestAckAlertAction.class).asEagerSingleton();
    }

}
