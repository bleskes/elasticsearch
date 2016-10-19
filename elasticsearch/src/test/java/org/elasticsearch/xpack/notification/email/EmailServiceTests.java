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

import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.common.secret.Secret;
import org.junit.Before;

import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EmailServiceTests extends ESTestCase {
    private EmailService service;
    private Account account;

    @Before
    public void init() throws Exception {
        account = mock(Account.class);
        service = new EmailService(Settings.builder().put("xpack.notification.email.account.account1.foo", "bar").build(), null,
                new ClusterSettings(Settings.EMPTY, Collections.singleton(EmailService.EMAIL_ACCOUNT_SETTING))) {
            @Override
            protected Account createAccount(String name, Settings accountSettings) {
                return account;
            }
        };
    }

    public void testSend() throws Exception {
        when(account.name()).thenReturn("account1");
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
