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

import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.common.text.TextTemplateEngine;
import org.elasticsearch.xpack.notification.slack.SlackService;
import org.elasticsearch.xpack.watcher.actions.ActionFactory;

import java.io.IOException;

public class SlackActionFactory extends ActionFactory {
    private final TextTemplateEngine templateEngine;
    private final SlackService slackService;

    public SlackActionFactory(Settings settings, TextTemplateEngine templateEngine, SlackService slackService) {
        super(Loggers.getLogger(ExecutableSlackAction.class, settings));
        this.templateEngine = templateEngine;
        this.slackService = slackService;
    }

    @Override
    public ExecutableSlackAction parseExecutable(String watchId, String actionId, XContentParser parser) throws IOException {
        SlackAction action = SlackAction.parse(watchId, actionId, parser);
        slackService.getAccount(action.account); // for validation -- throws exception if account not present
        return new ExecutableSlackAction(action, actionLogger, slackService, templateEngine);
    }
}
