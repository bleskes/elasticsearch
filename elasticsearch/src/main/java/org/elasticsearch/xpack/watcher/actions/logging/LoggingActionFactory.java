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

package org.elasticsearch.xpack.watcher.actions.logging;

import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.common.text.TextTemplateEngine;
import org.elasticsearch.xpack.watcher.actions.ActionFactory;

import java.io.IOException;

public class LoggingActionFactory extends ActionFactory {

    private final Settings settings;
    private final TextTemplateEngine templateEngine;

    public LoggingActionFactory(Settings settings, TextTemplateEngine templateEngine) {
        super(Loggers.getLogger(ExecutableLoggingAction.class, settings));
        this.settings = settings;
        this.templateEngine = templateEngine;
    }

    @Override
    public ExecutableLoggingAction parseExecutable(String watchId, String actionId, XContentParser parser) throws IOException {
        LoggingAction action = LoggingAction.parse(watchId, actionId, parser);
        return new ExecutableLoggingAction(action, actionLogger, settings, templateEngine);

    }
}
