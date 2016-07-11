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

import javax.mail.MessagingException;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.security.crypto.CryptoService;

/**
 *
 */
public class InternalEmailService extends AbstractLifecycleComponent implements EmailService {

    private final CryptoService cryptoService;
    public static final Setting<Settings> EMAIL_ACCOUNT_SETTING =
            Setting.groupSetting("xpack.notification.email.", Setting.Property.Dynamic, Setting.Property.NodeScope);

    private volatile Accounts accounts;

    @Inject
    public InternalEmailService(Settings settings, @Nullable CryptoService cryptoService, ClusterSettings clusterSettings) {
        super(settings);
        this.cryptoService = cryptoService;
        clusterSettings.addSettingsUpdateConsumer(EMAIL_ACCOUNT_SETTING, this::setEmailAccountSettings);
        setEmailAccountSettings(EMAIL_ACCOUNT_SETTING.get(settings));
    }

    private void setEmailAccountSettings(Settings settings) {
        this.accounts = createAccounts(settings, logger);
    }

    @Override
    protected void doStart() throws ElasticsearchException {
    }

    @Override
    protected void doStop() throws ElasticsearchException {
    }

    @Override
    protected void doClose() throws ElasticsearchException {
    }

    @Override
    public EmailSent send(Email email, Authentication auth, Profile profile) throws MessagingException {
        return send(email, auth, profile, (String) null);
    }

    @Override
    public EmailSent send(Email email, Authentication auth, Profile profile, String accountName) throws MessagingException {
        Account account = accounts.account(accountName);
        if (account == null) {
            throw new IllegalArgumentException("failed to send email with subject [" + email.subject() + "] via account [" + accountName
                    + "]. account does not exist");
        }
        return send(email, auth, profile, account);
    }

    EmailSent send(Email email, Authentication auth, Profile profile, Account account) throws MessagingException {
        assert account != null;
        try {
            email = account.send(email, auth, profile);
        } catch (MessagingException me) {
            throw new MessagingException("failed to send email with subject [" + email.subject() + "] via account [" + account.name() +
                    "]", me);
        }
        return new EmailSent(account.name(), email);
    }

    protected Accounts createAccounts(Settings settings, ESLogger logger) {
        return new Accounts(settings, cryptoService, logger);
    }

}
