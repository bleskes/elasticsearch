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

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.multibindings.MapBinder;
import org.elasticsearch.watcher.actions.email.EmailAction;
import org.elasticsearch.watcher.actions.email.EmailActionFactory;
import org.elasticsearch.watcher.actions.email.service.EmailService;
import org.elasticsearch.watcher.actions.email.service.HtmlSanitizer;
import org.elasticsearch.watcher.actions.email.service.InternalEmailService;
import org.elasticsearch.watcher.actions.hipchat.HipChatAction;
import org.elasticsearch.watcher.actions.hipchat.HipChatActionFactory;
import org.elasticsearch.watcher.actions.hipchat.service.HipChatService;
import org.elasticsearch.watcher.actions.hipchat.service.InternalHipChatService;
import org.elasticsearch.watcher.actions.index.IndexAction;
import org.elasticsearch.watcher.actions.index.IndexActionFactory;
import org.elasticsearch.watcher.actions.logging.LoggingAction;
import org.elasticsearch.watcher.actions.logging.LoggingActionFactory;
import org.elasticsearch.watcher.actions.webhook.WebhookAction;
import org.elasticsearch.watcher.actions.webhook.WebhookActionFactory;

import java.util.HashMap;
import java.util.Map;

/**
 */
public class WatcherActionModule extends AbstractModule {

    private final Map<String, Class<? extends ActionFactory>> parsers = new HashMap<>();

    public WatcherActionModule() {
        registerAction(EmailAction.TYPE, EmailActionFactory.class);
        registerAction(WebhookAction.TYPE, WebhookActionFactory.class);
        registerAction(IndexAction.TYPE, IndexActionFactory.class);
        registerAction(LoggingAction.TYPE, LoggingActionFactory.class);
        registerAction(HipChatAction.TYPE, HipChatActionFactory.class);
    }

    public void registerAction(String type, Class<? extends ActionFactory> parserType) {
        parsers.put(type, parserType);
    }

    @Override
    protected void configure() {

        MapBinder<String, ActionFactory> parsersBinder = MapBinder.newMapBinder(binder(), String.class, ActionFactory.class);
        for (Map.Entry<String, Class<? extends ActionFactory>> entry : parsers.entrySet()) {
            bind(entry.getValue()).asEagerSingleton();
            parsersBinder.addBinding(entry.getKey()).to(entry.getValue());
        }

        bind(ActionRegistry.class).asEagerSingleton();

        bind(HtmlSanitizer.class).asEagerSingleton();
        bind(EmailService.class).to(InternalEmailService.class).asEagerSingleton();

        bind(HipChatService.class).to(InternalHipChatService.class).asEagerSingleton();
    }


}
