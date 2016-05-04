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

package org.elasticsearch.xpack.watcher.actions.hipchat;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.watcher.actions.ActionFactory;
import org.elasticsearch.xpack.notification.hipchat.HipChatAccount;
import org.elasticsearch.xpack.notification.hipchat.HipChatService;
import org.elasticsearch.xpack.common.text.TextTemplateEngine;

import java.io.IOException;

/**
 *
 */
public class HipChatActionFactory extends ActionFactory<HipChatAction, ExecutableHipChatAction> {

    private final TextTemplateEngine templateEngine;
    private final HipChatService hipchatService;

    @Inject
    public HipChatActionFactory(Settings settings, TextTemplateEngine templateEngine, HipChatService hipchatService) {
        super(Loggers.getLogger(ExecutableHipChatAction.class, settings));
        this.templateEngine = templateEngine;
        this.hipchatService = hipchatService;
    }

    @Override
    public String type() {
        return HipChatAction.TYPE;
    }

    @Override
    public HipChatAction parseAction(String watchId, String actionId, XContentParser parser) throws IOException {
        HipChatAction action = HipChatAction.parse(watchId, actionId, parser);
        HipChatAccount account = hipchatService.getAccount(action.account);
        if (account == null) {
            throw new ElasticsearchParseException("could not parse [hipchat] action [{}]. unknown hipchat account [{}]", watchId,
                    action.account);
        }
        account.validateParsedTemplate(watchId, actionId, action.message);
        return action;
    }

    @Override
    public ExecutableHipChatAction createExecutable(HipChatAction action) {
        return new ExecutableHipChatAction(action, actionLogger,  hipchatService, templateEngine);
    }

}
