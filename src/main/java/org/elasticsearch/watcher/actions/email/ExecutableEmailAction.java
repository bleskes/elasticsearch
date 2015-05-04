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

package org.elasticsearch.watcher.actions.email;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.watcher.actions.ExecutableAction;
import org.elasticsearch.watcher.actions.email.service.Attachment;
import org.elasticsearch.watcher.actions.email.service.Email;
import org.elasticsearch.watcher.actions.email.service.EmailService;
import org.elasticsearch.watcher.execution.WatchExecutionContext;
import org.elasticsearch.watcher.support.Variables;
import org.elasticsearch.watcher.support.template.TemplateEngine;
import org.elasticsearch.watcher.watch.Payload;

import java.util.HashMap;
import java.util.Map;

/**
 */
public class ExecutableEmailAction extends ExecutableAction<EmailAction, EmailAction.Result> {

    final EmailService emailService;
    final TemplateEngine templateEngine;

    public ExecutableEmailAction(EmailAction action, ESLogger logger, EmailService emailService, TemplateEngine templateEngine) {
        super(action, logger);
        this.emailService = emailService;
        this.templateEngine = templateEngine;
    }

    protected EmailAction.Result doExecute(String actionId, WatchExecutionContext ctx, Payload payload) throws Exception {
        Map<String, Object> model = Variables.createCtxModel(ctx, payload);

        Map<String, Attachment> attachmentMap = new HashMap<>();
        Attachment.Bytes attachment = null;
        if (action.getAttachData()) {
            attachment = new Attachment.XContent.Yaml("data", "data.yml", new Payload.Simple(model));
            attachmentMap.put(attachment.id(), attachment);
        }

        Email.Builder email = action.getEmail().render(templateEngine, model, attachmentMap);
        email.id(ctx.id().value());
        if (attachment != null) {
            email.attach(attachment);
        }

        if (ctx.simulateAction(actionId)) {
            return new EmailAction.Result.Simulated(email.build());
        }

        EmailService.EmailSent sent = emailService.send(email.build(), action.getAuth(), action.getProfile(), action.getAccount());
        return new EmailAction.Result.Success(sent.account(), sent.email());
    }

    @Override
    protected EmailAction.Result failure(String reason) {
        return new EmailAction.Result.Failure(reason);
    }
}
