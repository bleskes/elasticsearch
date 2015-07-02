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

package org.elasticsearch.watcher.actions.email.service;

import org.elasticsearch.common.cli.Terminal;
import org.elasticsearch.common.inject.Provider;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.node.settings.NodeSettingsService;
import org.elasticsearch.watcher.shield.WatcherSettingsFilter;
import org.elasticsearch.watcher.support.secret.SecretService;
import org.junit.Ignore;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 */
@Ignore
public class ManualPublicSmtpServersTests {

    private static final Terminal terminal = Terminal.DEFAULT;

    public static class Gmail {

        public static void main(String[] args) throws Exception {
            test(Profile.GMAIL, Settings.builder()
                    .put("watcher.actions.email.service.account.gmail.smtp.auth", true)
                    .put("watcher.actions.email.service.account.gmail.smtp.starttls.enable", true)
                    .put("watcher.actions.email.service.account.gmail.smtp.host", "smtp.gmail.com")
                    .put("watcher.actions.email.service.account.gmail.smtp.port", 587)
                    .put("watcher.actions.email.service.account.gmail.smtp.user", terminal.readText("username: "))
                    .put("watcher.actions.email.service.account.gmail.smtp.password", new String(terminal.readSecret("password: ")))
                    .put("watcher.actions.email.service.account.gmail.email_defaults.to", terminal.readText("to: "))
            );
        }
    }

    public static class OutlookDotCom {

        public static void main(String[] args) throws Exception {
            test(Profile.STANDARD, Settings.builder()
                    .put("watcher.actions.email.service.account.outlook.smtp.auth", true)
                    .put("watcher.actions.email.service.account.outlook.smtp.starttls.enable", true)
                    .put("watcher.actions.email.service.account.outlook.smtp.host", "smtp-mail.outlook.com")
                    .put("watcher.actions.email.service.account.outlook.smtp.port", 587)
                    .put("watcher.actions.email.service.account.outlook.smtp.user", "elastic.user@outlook.com")
                    .put("watcher.actions.email.service.account.outlook.smtp.password", "fantastic42")
                    .put("watcher.actions.email.service.account.outlook.email_defaults.to", "elastic.user@outlook.com")
                    .put()
            );
        }
    }

    public static class YahooMail {

        public static void main(String[] args) throws Exception {
            test(Profile.STANDARD, Settings.builder()
                            .put("watcher.actions.email.service.account.yahoo.smtp.starttls.enable", true)
                            .put("watcher.actions.email.service.account.yahoo.smtp.auth", true)
                            .put("watcher.actions.email.service.account.yahoo.smtp.host", "smtp.mail.yahoo.com")
                            .put("watcher.actions.email.service.account.yahoo.smtp.port", 587)
                            .put("watcher.actions.email.service.account.yahoo.smtp.user", "elastic.user@yahoo.com")
                            .put("watcher.actions.email.service.account.yahoo.smtp.password", "fantastic42")
                            // note: from must be set to the same authenticated user account
                            .put("watcher.actions.email.service.account.yahoo.email_defaults.from", "elastic.user@yahoo.com")
                            .put("watcher.actions.email.service.account.yahoo.email_defaults.to", "elastic.user@yahoo.com")
            );
        }
    }

    // Amazon Simple Email Service
    public static class SES {

        public static void main(String[] args) throws Exception {
            test(Profile.STANDARD, Settings.builder()
                            .put("watcher.actions.email.service.account.ses.smtp.auth", true)
                            .put("watcher.actions.email.service.account.ses.smtp.starttls.enable", true)
                            .put("watcher.actions.email.service.account.ses.smtp.starttls.required", true)
                            .put("watcher.actions.email.service.account.ses.smtp.host", "email-smtp.us-east-1.amazonaws.com")
                            .put("watcher.actions.email.service.account.ses.smtp.port", 587)
                            .put("watcher.actions.email.service.account.ses.smtp.user", terminal.readText("user: "))
                            .put("watcher.actions.email.service.account.ses.smtp.password", new String(terminal.readSecret("password: ")))
                            .put("watcher.actions.email.service.account.ses.email_defaults.from", "dummy.user@elasticsearch.com")
                            .put("watcher.actions.email.service.account.ses.email_defaults.to", terminal.readText("to: "))
            );
        }
    }

    static void test(Profile profile, Settings.Builder builder) throws Exception {
        InternalEmailService service = startEmailService(builder);
        try {

            ToXContent content = new ToXContent() {
                @Override
                public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
                    return builder.startObject()
                            .field("key1", "value1")
                            .field("key2", "value2")
                            .field("key3", "value3")
                            .endObject();
                }
            };

            Email email = Email.builder()
                    .id("_id")
                    .subject("_subject")
                    .textBody("_text_body")
                    .htmlBody("<b>html body</b><p/><p/><img src=\"cid:logo\"/>")
                    .attach(new Attachment.XContent.Yaml("test.yml", content))
                    .inline(new Inline.Stream("logo", "logo.jpg", new Provider<InputStream>() {
                        @Override
                        public InputStream get() {
                            return InternalEmailServiceTests.class.getResourceAsStream("logo.png");
                        }
                    }))
                    .build();

            EmailService.EmailSent sent = service.send(email, null, profile);

            terminal.println("email sent via account [%s]", sent.account());
        } finally {
            service.stop();
        }
    }

    static InternalEmailService startEmailService(Settings.Builder builder) {
        Settings settings = builder.build();
        InternalEmailService service = new InternalEmailService(settings, new SecretService.PlainText(), new NodeSettingsService(settings), WatcherSettingsFilter.Noop.INSTANCE);
        service.start();
        return service;
    }
}
