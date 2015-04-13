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

package org.elasticsearch.watcher.actions.logging;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.watcher.actions.ExecutableAction;
import org.elasticsearch.watcher.execution.WatchExecutionContext;
import org.elasticsearch.watcher.support.Variables;
import org.elasticsearch.watcher.support.template.TemplateEngine;
import org.elasticsearch.watcher.watch.Payload;

import java.io.IOException;
import java.util.Map;

/**
 *
 */
public class ExecutableLoggingAction extends ExecutableAction<LoggingAction, LoggingAction.Result> {

    private final ESLogger textLogger;
    private final TemplateEngine templateEngine;

    ExecutableLoggingAction(LoggingAction action, ESLogger logger, Settings settings, TemplateEngine templateEngine) {
        super(action, logger);
        this.textLogger = action.category != null ? Loggers.getLogger(action.category, settings) : logger;
        this.templateEngine = templateEngine;
    }

    // for tests
    ExecutableLoggingAction(LoggingAction action, ESLogger logger, ESLogger textLogger, TemplateEngine templateEngine) {
        super(action, logger);
        this.textLogger = textLogger;
        this.templateEngine = templateEngine;
    }

    ESLogger textLogger() {
        return textLogger;
    }

    @Override
    protected LoggingAction.Result doExecute(String actionId, WatchExecutionContext ctx, Payload payload) throws IOException {
        Map<String, Object> model = Variables.createCtxModel(ctx, payload);

        String loggedText = templateEngine.render(action.text, model);
        if (ctx.simulateAction(actionId)) {
            return new LoggingAction.Result.Simulated(loggedText);
        }

        action.level.log(textLogger, loggedText);
        return new LoggingAction.Result.Success(loggedText);
    }

    @Override
    protected LoggingAction.Result failure(String reason) {
        return new LoggingAction.Result.Failure(reason);
    }
}
