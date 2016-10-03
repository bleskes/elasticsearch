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

package org.elasticsearch.xpack.watcher.actions.slack;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.common.text.TextTemplateEngine;
import org.elasticsearch.xpack.watcher.actions.ActionFactory;
import org.elasticsearch.xpack.watcher.actions.hipchat.ExecutableHipChatAction;
import org.elasticsearch.xpack.notification.slack.SlackAccount;
import org.elasticsearch.xpack.notification.slack.SlackService;

import java.io.IOException;

/**
 *
 */
public class SlackActionFactory extends ActionFactory<SlackAction, ExecutableSlackAction> {
    private final TextTemplateEngine templateEngine;
    private final SlackService slackService;

    @Inject
    public SlackActionFactory(Settings settings, TextTemplateEngine templateEngine, SlackService slackService) {
        super(Loggers.getLogger(ExecutableHipChatAction.class, settings));
        this.templateEngine = templateEngine;
        this.slackService = slackService;
    }

    @Override
    public String type() {
        return SlackAction.TYPE;
    }

    @Override
    public SlackAction parseAction(String watchId, String actionId, XContentParser parser) throws IOException {
        SlackAction action = SlackAction.parse(watchId, actionId, parser);
        SlackAccount account = slackService.getAccount(action.account);
        if (account == null) {
            throw new ElasticsearchParseException("could not parse [slack] action [{}]. unknown slack account [{}]", watchId,
                    action.account);
        }
        return action;
    }

    @Override
    public ExecutableSlackAction createExecutable(SlackAction action) {
        return new ExecutableSlackAction(action, actionLogger, slackService, templateEngine);
    }
}
