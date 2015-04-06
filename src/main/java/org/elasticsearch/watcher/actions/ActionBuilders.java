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
import org.elasticsearch.watcher.actions.index.IndexAction;
import org.elasticsearch.watcher.actions.logging.LoggingAction;
import org.elasticsearch.watcher.actions.webhook.WebhookAction;
import org.elasticsearch.watcher.support.http.TemplatedHttpRequest;
import org.elasticsearch.watcher.support.template.Template;

/**
 *
 */
public final class ActionBuilders {

    private ActionBuilders() {
    }

    public static EmailAction.SourceBuilder emailAction(String id) {
        return new EmailAction.SourceBuilder(id);
    }

    public static IndexAction.SourceBuilder indexAction(String id, String index, String type) {
        return new IndexAction.SourceBuilder(id, index, type);
    }

    public static WebhookAction.SourceBuilder webhookAction(String id, TemplatedHttpRequest.SourceBuilder httpRequest) {
        return new WebhookAction.SourceBuilder(id, httpRequest);
    }

    public static LoggingAction.SourceBuilder loggingAction(String id, String text) {
        return new LoggingAction.SourceBuilder(id).text(text);
    }

    public static LoggingAction.SourceBuilder loggingAction(String id, Template.SourceBuilder text) {
        return new LoggingAction.SourceBuilder(id).text(text);
    }

}
