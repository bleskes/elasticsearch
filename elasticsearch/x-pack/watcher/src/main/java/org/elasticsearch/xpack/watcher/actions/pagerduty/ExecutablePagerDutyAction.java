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

package org.elasticsearch.xpack.watcher.actions.pagerduty;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.xpack.watcher.actions.Action;
import org.elasticsearch.xpack.watcher.actions.ExecutableAction;
import org.elasticsearch.xpack.notification.pagerduty.PagerDutyAccount;
import org.elasticsearch.xpack.notification.pagerduty.PagerDutyService;
import org.elasticsearch.xpack.notification.pagerduty.SentEvent;
import org.elasticsearch.xpack.notification.pagerduty.IncidentEvent;
import org.elasticsearch.xpack.watcher.execution.WatchExecutionContext;
import org.elasticsearch.xpack.watcher.support.Variables;
import org.elasticsearch.xpack.common.text.TextTemplateEngine;
import org.elasticsearch.xpack.watcher.watch.Payload;

import java.util.Map;

/**
 *
 */
public class ExecutablePagerDutyAction extends ExecutableAction<PagerDutyAction> {

    private final TextTemplateEngine templateEngine;
    private final PagerDutyService pagerDutyService;

    public ExecutablePagerDutyAction(PagerDutyAction action, ESLogger logger, PagerDutyService pagerDutyService,
                                     TextTemplateEngine templateEngine) {
        super(action, logger);
        this.pagerDutyService = pagerDutyService;
        this.templateEngine = templateEngine;
    }

    @Override
    public Action.Result execute(final String actionId, WatchExecutionContext ctx, Payload payload) throws Exception {

        PagerDutyAccount account = action.event.account != null ?
                pagerDutyService.getAccount(action.event.account) :
                pagerDutyService.getDefaultAccount();

        if (account == null) {
            // the account associated with this action was deleted
            throw new IllegalStateException("account [" + action.event.account + "] was not found. perhaps it was deleted");
        }

        Map<String, Object> model = Variables.createCtxModel(ctx, payload);
        IncidentEvent event = action.event.render(ctx.watch().id(), actionId, templateEngine, model, account.getDefaults());

        if (ctx.simulateAction(actionId)) {
            return new PagerDutyAction.Result.Simulated(event);
        }

        SentEvent sentEvent = account.send(event, payload);
        return new PagerDutyAction.Result.Executed(account.getName(), sentEvent);
    }

}
