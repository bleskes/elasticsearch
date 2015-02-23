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

package org.elasticsearch.alerts.transport;

import org.elasticsearch.action.ActionModule;
import org.elasticsearch.alerts.transport.actions.ack.AckAlertAction;
import org.elasticsearch.alerts.transport.actions.ack.TransportAckAlertAction;
import org.elasticsearch.alerts.transport.actions.delete.DeleteAlertAction;
import org.elasticsearch.alerts.transport.actions.delete.TransportDeleteAlertAction;
import org.elasticsearch.alerts.transport.actions.get.GetAlertAction;
import org.elasticsearch.alerts.transport.actions.get.TransportGetAlertAction;
import org.elasticsearch.alerts.transport.actions.put.PutAlertAction;
import org.elasticsearch.alerts.transport.actions.put.TransportPutAlertAction;
import org.elasticsearch.alerts.transport.actions.service.AlertsServiceAction;
import org.elasticsearch.alerts.transport.actions.service.TransportAlertsServiceAction;
import org.elasticsearch.alerts.transport.actions.stats.AlertsStatsAction;
import org.elasticsearch.alerts.transport.actions.stats.TransportAlertsStatsAction;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.inject.PreProcessModule;

/**
 *
 */
public class AlertsTransportModule extends AbstractModule implements PreProcessModule {

    @Override
    public void processModule(Module module) {
        if (module instanceof ActionModule) {
            ActionModule actionModule = (ActionModule) module;
            actionModule.registerAction(PutAlertAction.INSTANCE, TransportPutAlertAction.class);
            actionModule.registerAction(DeleteAlertAction.INSTANCE, TransportDeleteAlertAction.class);
            actionModule.registerAction(GetAlertAction.INSTANCE, TransportGetAlertAction.class);
            actionModule.registerAction(AlertsStatsAction.INSTANCE, TransportAlertsStatsAction.class);
            actionModule.registerAction(AckAlertAction.INSTANCE, TransportAckAlertAction.class);
            actionModule.registerAction(AlertsServiceAction.INSTANCE, TransportAlertsServiceAction.class);
        }
    }

    @Override
    protected void configure() {
    }

}
