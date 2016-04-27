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

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.xpack.watcher.actions.Action;
import org.elasticsearch.xpack.watcher.actions.ExecutableAction;
import org.elasticsearch.xpack.watcher.execution.WatchExecutionContext;
import org.elasticsearch.xpack.watcher.support.Variables;
import org.elasticsearch.xpack.watcher.support.text.TextTemplateEngine;
import org.elasticsearch.xpack.watcher.watch.Payload;
import org.elasticsearch.xpack.notification.email.Attachment;
import org.elasticsearch.xpack.notification.email.DataAttachment;
import org.elasticsearch.xpack.notification.email.Email;
import org.elasticsearch.xpack.notification.email.EmailService;
import org.elasticsearch.xpack.notification.email.HtmlSanitizer;
import org.elasticsearch.xpack.notification.email.attachment.EmailAttachmentParser;

import java.util.HashMap;
import java.util.Map;

/**
 */
public class ExecutableEmailAction extends ExecutableAction<EmailAction> {

    final EmailService emailService;
    final TextTemplateEngine templateEngine;
    final HtmlSanitizer htmlSanitizer;
    private final Map<String, EmailAttachmentParser> emailAttachmentParsers;

    public ExecutableEmailAction(EmailAction action, ESLogger logger, EmailService emailService, TextTemplateEngine templateEngine,
                                 HtmlSanitizer htmlSanitizer, Map<String, EmailAttachmentParser> emailAttachmentParsers) {
        super(action, logger);
        this.emailService = emailService;
        this.templateEngine = templateEngine;
        this.htmlSanitizer = htmlSanitizer;
        this.emailAttachmentParsers = emailAttachmentParsers;
    }

    public Action.Result execute(String actionId, WatchExecutionContext ctx, Payload payload) throws Exception {
        Map<String, Object> model = Variables.createCtxModel(ctx, payload);

        Map<String, Attachment> attachments = new HashMap<>();
        DataAttachment dataAttachment = action.getDataAttachment();
        if (dataAttachment != null) {
            Attachment attachment = dataAttachment.create("data", model);
            attachments.put(attachment.id(), attachment);
        }

        if (action.getAttachments() != null && action.getAttachments().getAttachments().size() > 0) {
            for (EmailAttachmentParser.EmailAttachment emailAttachment : action.getAttachments().getAttachments()) {
                EmailAttachmentParser parser = emailAttachmentParsers.get(emailAttachment.type());
                try {
                    Attachment attachment = parser.toAttachment(ctx, payload, emailAttachment);
                    attachments.put(attachment.id(), attachment);
                } catch (ElasticsearchException e) {
                    return new EmailAction.Result.Failure(action.type(), e.getMessage());
                }
            }
        }

        Email.Builder email = action.getEmail().render(templateEngine, model, htmlSanitizer, attachments);
        email.id(ctx.id().value());

        if (ctx.simulateAction(actionId)) {
            return new EmailAction.Result.Simulated(email.build());
        }

        EmailService.EmailSent sent = emailService.send(email.build(), action.getAuth(), action.getProfile(), action.getAccount());
        return new EmailAction.Result.Success(sent.account(), sent.email());
    }
}
