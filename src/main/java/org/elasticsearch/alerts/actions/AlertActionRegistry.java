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

package org.elasticsearch.alerts.actions;

import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.alerts.Alert;
import org.elasticsearch.alerts.AlertsService;
import org.elasticsearch.alerts.ConfigurationService;
import org.elasticsearch.alerts.support.init.proxy.ClientProxy;
import org.elasticsearch.alerts.support.init.proxy.ScriptServiceProxy;
import org.elasticsearch.alerts.triggers.TriggerResult;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AlertActionRegistry extends AbstractComponent {

    private volatile ImmutableOpenMap<String, AlertActionFactory> actionImplemented;

    @Inject
    public AlertActionRegistry(Settings settings, ClientProxy client, ConfigurationService configurationService, ScriptServiceProxy scriptService) {
        super(settings);
        actionImplemented = ImmutableOpenMap.<String, AlertActionFactory>builder()
                .fPut("email", new SmtpAlertActionFactory(configurationService, scriptService))
                .fPut("index", new IndexAlertActionFactory(client, configurationService))
                .fPut("webhook", new WebhookAlertActionFactory(scriptService))
                .build();
    }

    public void registerAction(String name, AlertActionFactory actionFactory){
        actionImplemented = ImmutableOpenMap.builder(actionImplemented)
                .fPut(name, actionFactory)
                .build();
    }


    public AlertActions parse(XContentParser parser) throws IOException {
        List<AlertAction> actions = new ArrayList<>();
        ImmutableOpenMap<String, AlertActionFactory> actionImplemented = this.actionImplemented;
        String actionFactoryName = null;
        XContentParser.Token token;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                actionFactoryName = parser.currentName();
            } else if (token == XContentParser.Token.START_OBJECT) {
                AlertActionFactory factory = actionImplemented.get(actionFactoryName);
                if (factory != null) {
                    actions.add(factory.createAction(parser));
                } else {
                    throw new ElasticsearchIllegalArgumentException("No action exists with the name [" + actionFactoryName + "]");
                }
            }
        }
        return new AlertActions(actions);
    }

    public void doAction(Alert alert, AlertsService.AlertRun alertRun){
        for (AlertAction action : alert.actions()) {
            AlertActionFactory factory = actionImplemented.get(action.getActionName());
            if (factory != null) {
                factory.doAction(action, alert, alertRun);
            } else {
                throw new ElasticsearchIllegalArgumentException("No action exists with the name [" + action.getActionName() + "]");
            }
        }
    }

}
