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

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.settings.NodeSettingsService;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.watcher.shield.WatcherSettingsFilter;
import org.elasticsearch.watcher.support.secret.Secret;
import org.elasticsearch.watcher.support.secret.SecretService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class InternalEmailServiceTests extends ESTestCase {

    private InternalEmailService service;
    private Accounts accounts;

    @Before
    public void init() throws Exception {
        accounts = mock(Accounts.class);
        service = new InternalEmailService(Settings.EMPTY, new SecretService.PlainText(), new NodeSettingsService(Settings.EMPTY), WatcherSettingsFilter.Noop.INSTANCE) {
            @Override
            protected Accounts createAccounts(Settings settings, ESLogger logger) {
                return accounts;
            }
        };
        service.start();
    }

    @After
    public void cleanup() throws Exception {
        service.stop();
    }

    @Test
    public void testSend() throws Exception {
        Account account = mock(Account.class);
        when(account.name()).thenReturn("account1");
        when(accounts.account("account1")).thenReturn(account);
        Email email = mock(Email.class);

        Authentication auth = new Authentication("user", new Secret("passwd".toCharArray()));
        Profile profile = randomFrom(Profile.values());
        when(account.send(email, auth, profile)).thenReturn(email);
        EmailService.EmailSent sent = service.send(email, auth, profile, "account1");
        verify(account).send(email, auth, profile);
        assertThat(sent, notNullValue());
        assertThat(sent.email(), sameInstance(email));
        assertThat(sent.account(), is("account1"));
    }

}
