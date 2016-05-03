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

package org.elasticsearch.xpack.watcher.actions;

import org.elasticsearch.xpack.watcher.actions.email.EmailAction;
import org.elasticsearch.xpack.watcher.actions.hipchat.HipChatAction;
import org.elasticsearch.xpack.watcher.actions.index.IndexAction;
import org.elasticsearch.xpack.watcher.actions.logging.LoggingAction;
import org.elasticsearch.xpack.watcher.actions.pagerduty.PagerDutyAction;
import org.elasticsearch.xpack.notification.email.EmailTemplate;
import org.elasticsearch.xpack.notification.pagerduty.IncidentEvent;
import org.elasticsearch.xpack.watcher.actions.slack.SlackAction;
import org.elasticsearch.xpack.notification.slack.message.SlackMessage;
import org.elasticsearch.xpack.watcher.actions.webhook.WebhookAction;
import org.elasticsearch.xpack.watcher.support.http.HttpRequestTemplate;
import org.elasticsearch.xpack.watcher.support.text.TextTemplate;

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

    public static SlackAction.Builder slackAction(String account, SlackMessage.Template.Builder message) {
        return slackAction(account, message.build());
    }

    public static SlackAction.Builder slackAction(String account, SlackMessage.Template message) {
        return SlackAction.builder(account, message);
    }

    public static PagerDutyAction.Builder triggerPagerDutyAction(String account, String description) {
        return pagerDutyAction(IncidentEvent.templateBuilder(description).setAccount(account));
    }

    public static PagerDutyAction.Builder pagerDutyAction(IncidentEvent.Template.Builder event) {
        return PagerDutyAction.builder(event.build());
    }

    public static PagerDutyAction.Builder pagerDutyAction(IncidentEvent.Template event) {
        return PagerDutyAction.builder(event);
    }
}
