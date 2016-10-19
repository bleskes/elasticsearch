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
import org.elasticsearch.common.settings.SettingsException;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.notification.NotificationService;

import java.util.Collections;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.notNullValue;

/**
 *
 */
public class AccountsTests extends ESTestCase {
    public void testSingleAccount() throws Exception {
        Settings.Builder builder = Settings.builder()
                .put("default_account", "account1");
        addAccountSettings("account1", builder);
        EmailService service = new EmailService(builder.build(), null,
                new ClusterSettings(Settings.EMPTY, Collections.singleton(EmailService.EMAIL_ACCOUNT_SETTING)));
        Account account = service.getAccount("account1");
        assertThat(account, notNullValue());
        assertThat(account.name(), equalTo("account1"));
        account = service.getAccount(null); // falling back on the default
        assertThat(account, notNullValue());
        assertThat(account.name(), equalTo("account1"));
    }

    public void testSingleAccountNoExplicitDefault() throws Exception {
        Settings.Builder builder = Settings.builder();
        addAccountSettings("account1", builder);
        EmailService service = new EmailService(builder.build(), null,
                new ClusterSettings(Settings.EMPTY, Collections.singleton(EmailService.EMAIL_ACCOUNT_SETTING)));
        Account account = service.getAccount("account1");
        assertThat(account, notNullValue());
        assertThat(account.name(), equalTo("account1"));
        account = service.getAccount(null); // falling back on the default
        assertThat(account, notNullValue());
        assertThat(account.name(), equalTo("account1"));
    }

    public void testMultipleAccounts() throws Exception {
        Settings.Builder builder = Settings.builder()
                .put("xpack.notification.email.default_account", "account1");
        addAccountSettings("account1", builder);
        addAccountSettings("account2", builder);

        EmailService service = new EmailService(builder.build(), null,
                new ClusterSettings(Settings.EMPTY, Collections.singleton(EmailService.EMAIL_ACCOUNT_SETTING)));
        Account account = service.getAccount("account1");
        assertThat(account, notNullValue());
        assertThat(account.name(), equalTo("account1"));
        account = service.getAccount("account2");
        assertThat(account, notNullValue());
        assertThat(account.name(), equalTo("account2"));
        account = service.getAccount(null); // falling back on the default
        assertThat(account, notNullValue());
        assertThat(account.name(), equalTo("account1"));
    }

    public void testMultipleAccountsNoExplicitDefault() throws Exception {
        Settings.Builder builder = Settings.builder()
                .put("default_account", "account1");
        addAccountSettings("account1", builder);
        addAccountSettings("account2", builder);

        EmailService service = new EmailService(builder.build(), null,
                new ClusterSettings(Settings.EMPTY, Collections.singleton(EmailService.EMAIL_ACCOUNT_SETTING)));
        Account account = service.getAccount("account1");
        assertThat(account, notNullValue());
        assertThat(account.name(), equalTo("account1"));
        account = service.getAccount("account2");
        assertThat(account, notNullValue());
        assertThat(account.name(), equalTo("account2"));
        account = service.getAccount(null);
        assertThat(account, notNullValue());
        assertThat(account.name(), isOneOf("account1", "account2"));
    }

    public void testMultipleAccountsUnknownDefault() throws Exception {
        Settings.Builder builder = Settings.builder()
                .put("xpack.notification.email.default_account", "unknown");
        addAccountSettings("account1", builder);
        addAccountSettings("account2", builder);
        try {
            new EmailService(builder.build(), null,
                    new ClusterSettings(Settings.EMPTY, Collections.singleton(EmailService.EMAIL_ACCOUNT_SETTING)));
            fail("Expected SettingsException");
        } catch (SettingsException e) {
            assertThat(e.getMessage(), is("could not find default account [unknown]"));
        }
    }

    public void testNoAccount() throws Exception {
        Settings.Builder builder = Settings.builder();
        EmailService service = new EmailService(builder.build(), null,
                new ClusterSettings(Settings.EMPTY, Collections.singleton(EmailService.EMAIL_ACCOUNT_SETTING)));
        expectThrows(IllegalArgumentException.class, () -> service.getAccount(null));
    }

    public void testNoAccountWithDefaultAccount() throws Exception {
        Settings.Builder builder = Settings.builder()
                .put("xpack.notification.email.default_account", "unknown");
        try {
            new EmailService(builder.build(), null,
                    new ClusterSettings(Settings.EMPTY, Collections.singleton(EmailService.EMAIL_ACCOUNT_SETTING)));
            fail("Expected SettingsException");
        } catch (SettingsException e) {
            assertThat(e.getMessage(), is("could not find default account [unknown]"));
        }
    }

    private void addAccountSettings(String name, Settings.Builder builder) {
        builder.put("xpack.notification.email.account." + name + ".smtp.host", "_host");
    }
}
