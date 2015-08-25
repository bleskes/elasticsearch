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

package org.elasticsearch.watcher.actions;

import org.elasticsearch.watcher.actions.email.EmailAction;
import org.elasticsearch.watcher.actions.email.service.EmailTemplate;
import org.elasticsearch.watcher.actions.hipchat.HipChatAction;
import org.elasticsearch.watcher.actions.index.IndexAction;
import org.elasticsearch.watcher.actions.logging.LoggingAction;
import org.elasticsearch.watcher.actions.webhook.WebhookAction;
import org.elasticsearch.watcher.support.http.HttpRequestTemplate;
import org.elasticsearch.watcher.support.text.TextTemplate;

/**
 *
 */
public final class ActionBuilders {

    private ActionBuilders() {
    }

    public static EmailAction.Builder emailAction(EmailTemplate.Builder email) {
        return emailAction(email.build());
    }

    public static EmailAction.Builder emailAction(EmailTemplate email) {
        return EmailAction.builder(email);
    }

    public static IndexAction.Builder indexAction(String index, String type) {
        return IndexAction.builder(index, type);
    }

    public static WebhookAction.Builder webhookAction(HttpRequestTemplate.Builder httpRequest) {
        return webhookAction(httpRequest.build());
    }

    public static WebhookAction.Builder webhookAction(HttpRequestTemplate httpRequest) {
        return WebhookAction.builder(httpRequest);
    }

    public static LoggingAction.Builder loggingAction(String text) {
        return loggingAction(TextTemplate.inline(text));
    }

    public static LoggingAction.Builder loggingAction(TextTemplate.Builder text) {
        return loggingAction(text.build());
    }

    public static LoggingAction.Builder loggingAction(TextTemplate text) {
        return LoggingAction.builder(text);
    }

    public static HipChatAction.Builder hipchatAction(String message) {
        return hipchatAction(TextTemplate.inline(message));
    }

    public static HipChatAction.Builder hipchatAction(String account, String body) {
        return hipchatAction(account, TextTemplate.inline(body));
    }

    public static HipChatAction.Builder hipchatAction(TextTemplate.Builder body) {
        return hipchatAction(body.build());
    }

    public static HipChatAction.Builder hipchatAction(String account, TextTemplate.Builder body) {
        return hipchatAction(account, body.build());
    }

    public static HipChatAction.Builder hipchatAction(TextTemplate body) {
        return hipchatAction(null, body);
    }

    public static HipChatAction.Builder hipchatAction(String account, TextTemplate body) {
        return HipChatAction.builder(account, body);
    }
}
