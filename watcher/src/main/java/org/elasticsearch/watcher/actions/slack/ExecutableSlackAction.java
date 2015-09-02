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

package org.elasticsearch.watcher.actions.slack;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.watcher.actions.Action;
import org.elasticsearch.watcher.actions.ExecutableAction;
import org.elasticsearch.watcher.actions.slack.service.SentMessages;
import org.elasticsearch.watcher.actions.slack.service.SlackAccount;
import org.elasticsearch.watcher.actions.slack.service.message.SlackMessage;
import org.elasticsearch.watcher.actions.slack.service.SlackService;
import org.elasticsearch.watcher.execution.WatchExecutionContext;
import org.elasticsearch.watcher.support.Variables;
import org.elasticsearch.watcher.support.text.TextTemplateEngine;
import org.elasticsearch.watcher.watch.Payload;

import java.util.Map;

/**
 *
 */
public class ExecutableSlackAction extends ExecutableAction<SlackAction> {

    private final TextTemplateEngine templateEngine;
    private final SlackService slackService;

    public ExecutableSlackAction(SlackAction action, ESLogger logger, SlackService slackService, TextTemplateEngine templateEngine) {
        super(action, logger);
        this.slackService = slackService;
        this.templateEngine = templateEngine;
    }

    @Override
    public Action.Result execute(final String actionId, WatchExecutionContext ctx, Payload payload) throws Exception {

        SlackAccount account = action.account != null ?
                slackService.getAccount(action.account) :
                slackService.getDefaultAccount();

        if (account == null) {
            // the account associated with this action was deleted
            throw new IllegalStateException("account [" + action.account + "] was not found. perhaps it was deleted");
        }

        Map<String, Object> model = Variables.createCtxModel(ctx, payload);
        SlackMessage message = action.message.render(ctx.id().watchId(), actionId, templateEngine, model, account.getMessageDefaults());

        if (ctx.simulateAction(actionId)) {
            return new SlackAction.Result.Simulated(message);
        }

        SentMessages sentMessages = account.send(message);
        return new SlackAction.Result.Executed(sentMessages);
    }

}
