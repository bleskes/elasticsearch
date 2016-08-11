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

package org.elasticsearch.xpack.watcher.actions.email;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.common.text.TextTemplateEngine;
import org.elasticsearch.xpack.watcher.actions.ActionFactory;
import org.elasticsearch.xpack.notification.email.EmailService;
import org.elasticsearch.xpack.notification.email.HtmlSanitizer;
import org.elasticsearch.xpack.notification.email.attachment.EmailAttachmentsParser;

import java.io.IOException;

/**
 *
 */
public class EmailActionFactory extends ActionFactory<EmailAction, ExecutableEmailAction> {

    private final EmailService emailService;
    private final TextTemplateEngine templateEngine;
    private final HtmlSanitizer htmlSanitizer;
    private final EmailAttachmentsParser emailAttachmentsParser;

    @Inject
    public EmailActionFactory(Settings settings, EmailService emailService, TextTemplateEngine templateEngine,
                              EmailAttachmentsParser emailAttachmentsParser) {
        super(Loggers.getLogger(ExecutableEmailAction.class, settings));
        this.emailService = emailService;
        this.templateEngine = templateEngine;
        this.htmlSanitizer = new HtmlSanitizer(settings);
        this.emailAttachmentsParser = emailAttachmentsParser;
    }

    @Override
    public String type() {
        return EmailAction.TYPE;
    }

    @Override
    public EmailAction parseAction(String watchId, String actionId, XContentParser parser) throws IOException {
        return EmailAction.parse(watchId, actionId, parser, emailAttachmentsParser);
    }

    @Override
    public ExecutableEmailAction createExecutable(EmailAction action) {
        return new ExecutableEmailAction(action, actionLogger, emailService, templateEngine, htmlSanitizer,
                emailAttachmentsParser.getParsers());
    }
}
