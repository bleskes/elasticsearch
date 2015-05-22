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

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.watcher.actions.Action;
import org.elasticsearch.watcher.actions.ActionFactory;
import org.elasticsearch.watcher.execution.Wid;
import org.elasticsearch.watcher.support.template.TemplateEngine;

import java.io.IOException;

/**
 *
 */
public class LoggingActionFactory extends ActionFactory<LoggingAction, ExecutableLoggingAction> {

    private final Settings settings;
    private final TemplateEngine templateEngine;

    @Inject
    public LoggingActionFactory(Settings settings, TemplateEngine templateEngine) {
        super(Loggers.getLogger(ExecutableLoggingAction.class, settings));
        this.settings = settings;
        this.templateEngine = templateEngine;
    }

    @Override
    public String type() {
        return LoggingAction.TYPE;
    }

    @Override
    public LoggingAction parseAction(String watchId, String actionId, XContentParser parser) throws IOException {
        return LoggingAction.parse(watchId, actionId, parser);
    }

    @Override
    public Action.Result parseResult(Wid wid, String actionId, XContentParser parser) throws IOException {
        return LoggingAction.parseResult(wid.watchId(), actionId, parser);
    }

    @Override
    public ExecutableLoggingAction createExecutable(LoggingAction action) {
        return new ExecutableLoggingAction(action, actionLogger, settings, templateEngine);
    }
}
