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

import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.notification.NotificationService;
import org.elasticsearch.xpack.security.crypto.CryptoService;

import javax.mail.MessagingException;

/**
 * A component to store email credentials and handle sending email notifications.
 */
public class EmailService extends NotificationService<Account> {

    private final CryptoService cryptoService;
    public static final Setting<Settings> EMAIL_ACCOUNT_SETTING =
        Setting.groupSetting("xpack.notification.email.", Setting.Property.Dynamic, Setting.Property.NodeScope);

    public EmailService(Settings settings, @Nullable CryptoService cryptoService, ClusterSettings clusterSettings) {
        super(settings);
        this.cryptoService = cryptoService;
        clusterSettings.addSettingsUpdateConsumer(EMAIL_ACCOUNT_SETTING, this::setAccountSetting);
        setAccountSetting(EMAIL_ACCOUNT_SETTING.get(settings));
    }

    @Override
    protected Account createAccount(String name, Settings accountSettings) {
        Account.Config config = new Account.Config(name, accountSettings);
        return new Account(config, cryptoService, logger);
    }


    public EmailSent send(Email email, Authentication auth, Profile profile, String accountName) throws MessagingException {
        Account account = getAccount(accountName);
        if (account == null) {
            throw new IllegalArgumentException("failed to send email with subject [" + email.subject() + "] via account [" + accountName
                + "]. account does not exist");
        }
        return send(email, auth, profile, account);
    }

    private EmailSent send(Email email, Authentication auth, Profile profile, Account account) throws MessagingException {
        assert account != null;
        try {
            email = account.send(email, auth, profile);
        } catch (MessagingException me) {
            throw new MessagingException("failed to send email with subject [" + email.subject() + "] via account [" + account.name() +
                "]", me);
        }
        return new EmailSent(account.name(), email);
    }

    public static class EmailSent {

        private final String account;
        private final Email email;

        public EmailSent(String account, Email email) {
            this.account = account;
            this.email = email;
        }

        public String account() {
            return account;
        }

        public Email email() {
            return email;
        }
    }

}
