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


import org.elasticsearch.alerts.actions.AlertActionManager;
import org.elasticsearch.alerts.actions.AlertActionRegistry;
import org.elasticsearch.alerts.client.NodeAlertsClient;
import org.elasticsearch.alerts.client.AlertsClient;
import org.elasticsearch.alerts.rest.RestAlertsStatsAction;
import org.elasticsearch.alerts.rest.RestDeleteAlertAction;
import org.elasticsearch.alerts.rest.RestGetAlertAction;
import org.elasticsearch.alerts.rest.RestPutAlertAction;
import org.elasticsearch.alerts.scheduler.AlertScheduler;
import org.elasticsearch.alerts.transport.actions.ack.TransportAckAlertAction;
import org.elasticsearch.alerts.transport.actions.delete.TransportDeleteAlertAction;
import org.elasticsearch.alerts.transport.actions.get.TransportGetAlertAction;
import org.elasticsearch.alerts.transport.actions.put.TransportPutAlertAction;
import org.elasticsearch.alerts.transport.actions.stats.TransportAlertStatsAction;
import org.elasticsearch.alerts.triggers.TriggerManager;
import org.elasticsearch.common.inject.AbstractModule;


public class AlertingModule extends AbstractModule {

    @Override
    protected void configure() {
        // Core components
        bind(TemplateHelper.class).asEagerSingleton();
        bind(AlertsStore.class).asEagerSingleton();
        bind(AlertManager.class).asEagerSingleton();
        bind(AlertActionManager.class).asEagerSingleton();
        bind(TriggerManager.class).asEagerSingleton();
        bind(AlertScheduler.class).asEagerSingleton();
        bind(AlertActionRegistry.class).asEagerSingleton();

        // Transport and client layer
        bind(TransportPutAlertAction.class).asEagerSingleton();
        bind(TransportDeleteAlertAction.class).asEagerSingleton();
        bind(TransportGetAlertAction.class).asEagerSingleton();
        bind(TransportAlertStatsAction.class).asEagerSingleton();
        bind(TransportAckAlertAction.class).asEagerSingleton();
        bind(AlertsClient.class).to(NodeAlertsClient.class).asEagerSingleton();

        // Rest layer
        bind(RestPutAlertAction.class).asEagerSingleton();
        bind(RestDeleteAlertAction.class).asEagerSingleton();
        bind(RestAlertsStatsAction.class).asEagerSingleton();
        bind(RestGetAlertAction.class).asEagerSingleton();
    }

}
