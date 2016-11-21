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

package org.elasticsearch.xpack.watcher.actions.jira;

import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.common.text.TextTemplateEngine;
import org.elasticsearch.xpack.notification.jira.JiraService;
import org.elasticsearch.xpack.watcher.actions.ActionFactory;

import java.io.IOException;

public class JiraActionFactory extends ActionFactory {

    private final TextTemplateEngine templateEngine;
    private final JiraService jiraService;

    public JiraActionFactory(Settings settings, TextTemplateEngine templateEngine, JiraService jiraService) {
        super(Loggers.getLogger(ExecutableJiraAction.class, settings));
        this.templateEngine = templateEngine;
        this.jiraService = jiraService;
    }

    @Override
    public ExecutableJiraAction parseExecutable(String watchId, String actionId, XContentParser parser) throws IOException {
        JiraAction action = JiraAction.parse(watchId, actionId, parser);
        jiraService.getAccount(action.getAccount()); // for validation -- throws exception if account not present
        return new ExecutableJiraAction(action, actionLogger, jiraService, templateEngine);
    }
}
