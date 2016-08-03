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

package org.elasticsearch.xpack.watcher.actions.hipchat;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.xpack.common.text.TextTemplateEngine;
import org.elasticsearch.xpack.watcher.actions.Action;
import org.elasticsearch.xpack.watcher.actions.ExecutableAction;
import org.elasticsearch.xpack.notification.hipchat.HipChatAccount;
import org.elasticsearch.xpack.notification.hipchat.HipChatMessage;
import org.elasticsearch.xpack.notification.hipchat.HipChatService;
import org.elasticsearch.xpack.notification.hipchat.SentMessages;
import org.elasticsearch.xpack.watcher.execution.WatchExecutionContext;
import org.elasticsearch.xpack.watcher.support.Variables;
import org.elasticsearch.xpack.watcher.watch.Payload;

import java.util.Map;

/**
 *
 */
public class ExecutableHipChatAction extends ExecutableAction<HipChatAction> {

    private final TextTemplateEngine templateEngine;
    private final HipChatService hipchatService;

    public ExecutableHipChatAction(HipChatAction action, ESLogger logger, HipChatService hipchatService,
                                   TextTemplateEngine templateEngine) {
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
