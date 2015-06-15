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

import com.google.common.collect.ImmutableMap;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.settings.NodeSettingsService;
import org.elasticsearch.watcher.shield.WatcherSettingsFilter;
import org.elasticsearch.watcher.support.secret.SecretService;

import javax.mail.MessagingException;

/**
 *
 */
public class InternalEmailService extends AbstractLifecycleComponent<InternalEmailService> implements EmailService {

    private final SecretService secretService;

    private volatile Accounts accounts;

    @Inject
    public InternalEmailService(Settings settings, SecretService secretService, NodeSettingsService nodeSettingsService, WatcherSettingsFilter settingsFilter) {
        super(settings);
        this.secretService = secretService;
        nodeSettingsService.addListener(new NodeSettingsService.Listener() {
            @Override
            public void onRefreshSettings(Settings settings) {
                reset(settings);
            }
        });
        settingsFilter.filterOut("watcher.actions.email.service.account.*.smtp.password");
    }

    @Override
    protected void doStart() throws ElasticsearchException {
        reset(settings);
    }

    @Override
    protected void doStop() throws ElasticsearchException {
    }

    @Override
    protected void doClose() throws ElasticsearchException {
    }

    @Override
    public EmailSent send(Email email, Authentication auth, Profile profile) {
        return send(email, auth, profile, (String) null);
    }

    @Override
    public EmailSent send(Email email, Authentication auth, Profile profile, String accountName) {
        Account account = accounts.account(accountName);
        if (account == null) {
            throw new EmailException("failed to send email with subject [" + email.subject() + "] via account [" + accountName + "]. account does not exist");
        }
        return send(email, auth, profile, account);
    }

    EmailSent send(Email email, Authentication auth, Profile profile, Account account) {
        assert account != null;
        try {
            email = account.send(email, auth, profile);
        } catch (MessagingException me) {
            throw new EmailException("failed to send email with subject [" + email.subject() + "] via account [" + account.name() + "]", me);
        }
        return new EmailSent(account.name(), email);
    }

    void reset(Settings nodeSettings) {
        Settings.Builder builder = Settings.builder();
        for (String setting : settings.getAsMap().keySet()) {
            if (setting.startsWith("watcher.actions.email.service")) {
                builder.put(setting, settings.get(setting));
            }
        }
        for (String setting : nodeSettings.getAsMap().keySet()) {
            if (setting.startsWith("watcher.actions.email.service")) {
                builder.put(setting, settings.get(setting));
            }
        }
        accounts = createAccounts(builder.build(), logger);
    }

    protected Accounts createAccounts(Settings settings, ESLogger logger) {
        return new Accounts(settings, secretService, logger);
    }

}
