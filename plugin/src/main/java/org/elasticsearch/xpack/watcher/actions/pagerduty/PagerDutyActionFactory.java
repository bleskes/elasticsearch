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

package org.elasticsearch.xpack.watcher.actions.pagerduty;

import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.common.text.TextTemplateEngine;
import org.elasticsearch.xpack.notification.pagerduty.PagerDutyService;
import org.elasticsearch.xpack.watcher.actions.ActionFactory;

import java.io.IOException;

public class PagerDutyActionFactory extends ActionFactory {

    private final TextTemplateEngine templateEngine;
    private final PagerDutyService pagerDutyService;

    public PagerDutyActionFactory(Settings settings, TextTemplateEngine templateEngine, PagerDutyService pagerDutyService) {
        super(Loggers.getLogger(ExecutablePagerDutyAction.class, settings));
        this.templateEngine = templateEngine;
        this.pagerDutyService = pagerDutyService;
    }

    @Override
    public ExecutablePagerDutyAction parseExecutable(String watchId, String actionId, XContentParser parser) throws IOException {
        PagerDutyAction action = PagerDutyAction.parse(watchId, actionId, parser);
        pagerDutyService.getAccount(action.event.account);
        return new ExecutablePagerDutyAction(action, actionLogger, pagerDutyService, templateEngine);
    }
}
