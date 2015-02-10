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

package org.elasticsearch.alerts.actions.email.service;

import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.settings.NodeSettingsService;

import javax.mail.MessagingException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
public class InternalEmailService extends AbstractComponent implements EmailService {

    private volatile Accounts accounts;

    private final AtomicBoolean started = new AtomicBoolean(false);

    @Inject
    public InternalEmailService(Settings settings, NodeSettingsService nodeSettingsService) {
        super(settings);
        nodeSettingsService.addListener(new NodeSettingsService.Listener() {
            @Override
            public void onRefreshSettings(Settings settings) {
                reset(settings);
            }
        });
    }

    @Override
    public EmailSent send(Email email, Authentication auth, Profile profile) {
        return send(email, auth, profile, (String) null);
    }

    @Override
    public EmailSent send(Email email, Authentication auth, Profile profile, String accountName) {
        Account account = accounts.account(accountName);
        if (account == null) {
            throw new EmailException("failed to send email [" + email + "] via account [" + accountName + "]. account does not exist");
        }
        return send(email, auth, profile, account);
    }

    EmailSent send(Email email, Authentication auth, Profile profile, Account account) {
        assert account != null;
        try {
            account.send(email, auth, profile);
        } catch (MessagingException me) {
            throw new EmailException("failed to send email [" + email + "] via account [" + account.name() + "]", me);
        }
        return new EmailSent(account.name(), email);
    }

    @Override
    public synchronized void start(ClusterState state) {
        if (started.get()) {
            return;
        }
        reset(state.metaData().settings());
        started.set(true);
    }

    @Override
    public synchronized void stop() {
        started.set(false);
    }

    synchronized void reset(Settings nodeSettings) {
        if (!started.get()) {
            return;
        }
        Settings settings = ImmutableSettings.builder()
                .put(componentSettings)
                .put(nodeSettings.getComponentSettings(InternalEmailService.class))
                .build();
        accounts = new Accounts(settings, logger);
    }

}
