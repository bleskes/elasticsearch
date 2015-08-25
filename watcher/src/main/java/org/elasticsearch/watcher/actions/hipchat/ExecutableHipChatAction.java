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

package org.elasticsearch.watcher.actions.hipchat;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.watcher.actions.Action;
import org.elasticsearch.watcher.actions.ExecutableAction;
import org.elasticsearch.watcher.actions.hipchat.service.HipChatAccount;
import org.elasticsearch.watcher.actions.hipchat.service.HipChatMessage;
import org.elasticsearch.watcher.actions.hipchat.service.HipChatService;
import org.elasticsearch.watcher.actions.hipchat.service.SentMessages;
import org.elasticsearch.watcher.execution.WatchExecutionContext;
import org.elasticsearch.watcher.support.Variables;
import org.elasticsearch.watcher.support.template.TemplateEngine;
import org.elasticsearch.watcher.watch.Payload;

import java.util.Map;

/**
 *
 */
public class ExecutableHipChatAction extends ExecutableAction<HipChatAction> {

    private final TemplateEngine templateEngine;
    private final HipChatService hipchatService;

    public ExecutableHipChatAction(HipChatAction action, ESLogger logger, HipChatService hipchatService, TemplateEngine templateEngine) {
        super(action, logger);
        this.hipchatService = hipchatService;
        this.templateEngine = templateEngine;
    }

    @Override
    public Action.Result execute(final String actionId, WatchExecutionContext ctx, Payload payload) throws Exception {

        HipChatAccount account = action.account != null ?
                hipchatService.getAccount(action.account) :
                hipchatService.getDefaultAccount();

        // lets validate the message again, in case the hipchat service were updated since the
        // watch/action were created.
        account.validateParsedTemplate(ctx.id().watchId(), actionId, action.message);

        Map<String, Object> model = Variables.createCtxModel(ctx, payload);
        HipChatMessage message = account.render(ctx.id().watchId(), actionId, templateEngine, action.message, model);

        if (ctx.simulateAction(actionId)) {
            return new HipChatAction.Result.Simulated(message);
        }

        SentMessages sentMessages = account.send(message);
        return new HipChatAction.Result.Executed(sentMessages);
    }

}
