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

package org.elasticsearch.xpack.notification;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.multibindings.MapBinder;
import org.elasticsearch.xpack.notification.email.EmailService;
import org.elasticsearch.xpack.notification.email.HtmlSanitizer;
import org.elasticsearch.xpack.notification.email.InternalEmailService;
import org.elasticsearch.xpack.notification.email.attachment.DataAttachmentParser;
import org.elasticsearch.xpack.notification.email.attachment.EmailAttachmentParser;
import org.elasticsearch.xpack.notification.email.attachment.EmailAttachmentsParser;
import org.elasticsearch.xpack.notification.email.attachment.HttpEmailAttachementParser;
import org.elasticsearch.xpack.notification.hipchat.HipChatService;
import org.elasticsearch.xpack.notification.hipchat.InternalHipChatService;
import org.elasticsearch.xpack.notification.pagerduty.InternalPagerDutyService;
import org.elasticsearch.xpack.notification.pagerduty.PagerDutyService;
import org.elasticsearch.xpack.notification.slack.InternalSlackService;
import org.elasticsearch.xpack.notification.slack.SlackService;

import java.util.HashMap;
import java.util.Map;

public class NotificationModule extends AbstractModule {

    private final Map<String, Class<? extends EmailAttachmentParser>> emailAttachmentParsers = new HashMap<>();


    public NotificationModule() {
        registerEmailAttachmentParser(HttpEmailAttachementParser.TYPE, HttpEmailAttachementParser.class);
        registerEmailAttachmentParser(DataAttachmentParser.TYPE, DataAttachmentParser.class);
    }

    public void registerEmailAttachmentParser(String type, Class<? extends EmailAttachmentParser> parserClass) {
        emailAttachmentParsers.put(type, parserClass);
    }

    @Override
    protected void configure() {
        // email
        bind(InternalEmailService.class).asEagerSingleton();
        bind(EmailService.class).to(InternalEmailService.class).asEagerSingleton();

        MapBinder<String, EmailAttachmentParser> emailParsersBinder = MapBinder.newMapBinder(binder(), String.class,
                EmailAttachmentParser.class);
        for (Map.Entry<String, Class<? extends EmailAttachmentParser>> entry : emailAttachmentParsers.entrySet()) {
            emailParsersBinder.addBinding(entry.getKey()).to(entry.getValue()).asEagerSingleton();
        }
        bind(EmailAttachmentsParser.class).asEagerSingleton();

        // hipchat
        bind(InternalHipChatService.class).asEagerSingleton();
        bind(HipChatService.class).to(InternalHipChatService.class);

        // slack
        bind(InternalSlackService.class).asEagerSingleton();
        bind(SlackService.class).to(InternalSlackService.class);

        // pager duty
        bind(InternalPagerDutyService.class).asEagerSingleton();
        bind(PagerDutyService.class).to(InternalPagerDutyService.class);
    }
}
