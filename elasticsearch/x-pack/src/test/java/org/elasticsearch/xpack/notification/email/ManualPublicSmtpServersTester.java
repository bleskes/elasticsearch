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

package org.elasticsearch.xpack.notification.email;

import org.apache.lucene.util.LuceneTestCase.AwaitsFix;
import org.elasticsearch.cli.Terminal;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.xpack.common.secret.SecretService;

import java.io.InputStream;
import java.util.Collections;
import java.util.Locale;

import static com.google.common.base.Preconditions.checkNotNull;

@AwaitsFix(bugUrl = "https://github.com/elastic/x-plugins/issues/379")
public class ManualPublicSmtpServersTester {

    private static final Terminal terminal = Terminal.DEFAULT;

    public static class Gmail {

        public static void main(String[] args) throws Exception {
            test(Profile.GMAIL, Settings.builder()
                    .put("xpack.notification.email.account.gmail.smtp.auth", true)
                    .put("xpack.notification.email.account.gmail.smtp.starttls.enable", true)
                    .put("xpack.notification.email.account.gmail.smtp.host", "smtp.gmail.com")
                    .put("xpack.notification.email.account.gmail.smtp.port", 587)
                    .put("xpack.notification.email.account.gmail.smtp.user", terminal.readText("username: "))
                    .put("xpack.notification.email.account.gmail.smtp.password", new String(terminal.readSecret("password: ")))
                    .put("xpack.notification.email.account.gmail.email_defaults.to", terminal.readText("to: "))
            );
        }
    }

    public static class OutlookDotCom {

        public static void main(String[] args) throws Exception {
            test(Profile.STANDARD, Settings.builder()
                    .put("xpack.notification.email.account.outlook.smtp.auth", true)
                    .put("xpack.notification.email.account.outlook.smtp.starttls.enable", true)
                    .put("xpack.notification.email.account.outlook.smtp.host", "smtp-mail.outlook.com")
                    .put("xpack.notification.email.account.outlook.smtp.port", 587)
                    .put("xpack.notification.email.account.outlook.smtp.user", "elastic.user@outlook.com")
                    .put("xpack.notification.email.account.outlook.smtp.password", "fantastic42")
                    .put("xpack.notification.email.account.outlook.email_defaults.to", "elastic.user@outlook.com")
                    .put()
            );
        }
    }

    public static class YahooMail {

        public static void main(String[] args) throws Exception {
            test(Profile.STANDARD, Settings.builder()
                            .put("xpack.notification.email.account.yahoo.smtp.starttls.enable", true)
                            .put("xpack.notification.email.account.yahoo.smtp.auth", true)
                            .put("xpack.notification.email.account.yahoo.smtp.host", "smtp.mail.yahoo.com")
                            .put("xpack.notification.email.account.yahoo.smtp.port", 587)
                            .put("xpack.notification.email.account.yahoo.smtp.user", "elastic.user@yahoo.com")
                            .put("xpack.notification.email.account.yahoo.smtp.password", "fantastic42")
                            // note: from must be set to the same authenticated user account
                            .put("xpack.notification.email.account.yahoo.email_defaults.from", "elastic.user@yahoo.com")
                            .put("xpack.notification.email.account.yahoo.email_defaults.to", "elastic.user@yahoo.com")
            );
        }
    }

    // Amazon Simple Email Service
    public static class SES {

        public static void main(String[] args) throws Exception {
            test(Profile.STANDARD, Settings.builder()
                            .put("xpack.notification.email.account.ses.smtp.auth", true)
                            .put("xpack.notification.email.account.ses.smtp.starttls.enable", true)
                            .put("xpack.notification.email.account.ses.smtp.starttls.required", true)
                            .put("xpack.notification.email.account.ses.smtp.host", "email-smtp.us-east-1.amazonaws.com")
                            .put("xpack.notification.email.account.ses.smtp.port", 587)
                            .put("xpack.notification.email.account.ses.smtp.user", terminal.readText("user: "))
                            .put("xpack.notification.email.account.ses.email_defaults.from", "dummy.user@elasticsearch.com")
                            .put("xpack.notification.email.account.ses.email_defaults.to", terminal.readText("to: "))
                            .put("xpack.notification.email.account.ses.smtp.password",
                                    new String(terminal.readSecret("password: ")))
            );
        }
    }

    static void test(Profile profile, Settings.Builder settingsBuilder) throws Exception {
        String path = "/org/elasticsearch/xpack/watcher/actions/email/service/logo.png";
        checkNotNull(InternalEmailServiceTests.class.getResourceAsStream(path));

        InternalEmailService service = startEmailService(settingsBuilder);
        try {

            ToXContent content = (xContentBuilder, params) -> xContentBuilder.startObject()
                    .field("key1", "value1")
                    .field("key2", "value2")
                    .field("key3", "value3")
                    .endObject();

            Email email = Email.builder()
                    .id("_id")
                    .subject("_subject")
                    .textBody("_text_body")
                    .htmlBody("<b>html body</b><p/><p/><img src=\"cid:logo.png\"/>")
                    .attach(new Attachment.XContent.Yaml("test.yml", content))
                    .inline(new Inline.Stream("logo.png", "logo.png", () -> InternalEmailServiceTests.class.getResourceAsStream(path)))
                    .build();

            EmailService.EmailSent sent = service.send(email, null, profile);

            terminal.println(String.format(Locale.ROOT, "email sent via account [%s]", sent.account()));
        } finally {
            service.stop();
        }
    }

    static InternalEmailService startEmailService(Settings.Builder builder) {
        Settings settings = builder.build();
        InternalEmailService service = new InternalEmailService(settings, SecretService.Insecure.INSTANCE,
                new ClusterSettings(settings, Collections.singleton(InternalEmailService.EMAIL_ACCOUNT_SETTING)));
        service.start();
        return service;
    }
}
