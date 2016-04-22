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

package org.elasticsearch.watcher.actions.pagerduty;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.watcher.actions.ActionFactory;
import org.elasticsearch.watcher.actions.hipchat.ExecutableHipChatAction;
import org.elasticsearch.xpack.notification.pagerduty.PagerDutyAccount;
import org.elasticsearch.xpack.notification.pagerduty.PagerDutyService;
import org.elasticsearch.watcher.support.text.TextTemplateEngine;

import java.io.IOException;

/**
 *
 */
public class PagerDutyActionFactory extends ActionFactory<PagerDutyAction, ExecutablePagerDutyAction> {

    private final TextTemplateEngine templateEngine;
    private final PagerDutyService pagerDutyService;

    @Inject
    public PagerDutyActionFactory(Settings settings, TextTemplateEngine templateEngine, PagerDutyService pagerDutyService) {
        super(Loggers.getLogger(ExecutableHipChatAction.class, settings));
        this.templateEngine = templateEngine;
        this.pagerDutyService = pagerDutyService;
    }

    @Override
    public String type() {
        return PagerDutyAction.TYPE;
    }

    @Override
    public PagerDutyAction parseAction(String watchId, String actionId, XContentParser parser) throws IOException {
        PagerDutyAction action = PagerDutyAction.parse(watchId, actionId, parser);
        PagerDutyAccount account = pagerDutyService.getAccount(action.event.account);
        if (account == null) {
            throw new ElasticsearchParseException("could not parse [pagerduty] action [{}/{}]. unknown pager duty account [{}]", watchId,
                    account, action.event.account);
        }
        return action;
    }

    @Override
    public ExecutablePagerDutyAction createExecutable(PagerDutyAction action) {
        return new ExecutablePagerDutyAction(action, actionLogger, pagerDutyService, templateEngine);
    }

}
