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

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsException;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.watcher.support.secret.SecretService;

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

        Accounts accounts = new Accounts(builder.build(), SecretService.Insecure.INSTANCE, logger);
        Account account = accounts.account("account1");
        assertThat(account, notNullValue());
        assertThat(account.name(), equalTo("account1"));
        account = accounts.account(null); // falling back on the default
        assertThat(account, notNullValue());
        assertThat(account.name(), equalTo("account1"));
    }

    public void testSingleAccountNoExplicitDefault() throws Exception {
        Settings.Builder builder = Settings.builder();
        addAccountSettings("account1", builder);

        Accounts accounts = new Accounts(builder.build(), SecretService.Insecure.INSTANCE, logger);
        Account account = accounts.account("account1");
        assertThat(account, notNullValue());
        assertThat(account.name(), equalTo("account1"));
        account = accounts.account(null); // falling back on the default
        assertThat(account, notNullValue());
        assertThat(account.name(), equalTo("account1"));
    }

    public void testMultipleAccounts() throws Exception {
        Settings.Builder builder = Settings.builder()
                .put("default_account", "account1");
        addAccountSettings("account1", builder);
        addAccountSettings("account2", builder);

        Accounts accounts = new Accounts(builder.build(), SecretService.Insecure.INSTANCE, logger);
        Account account = accounts.account("account1");
        assertThat(account, notNullValue());
        assertThat(account.name(), equalTo("account1"));
        account = accounts.account("account2");
        assertThat(account, notNullValue());
        assertThat(account.name(), equalTo("account2"));
        account = accounts.account(null); // falling back on the default
        assertThat(account, notNullValue());
        assertThat(account.name(), equalTo("account1"));
    }

    public void testMultipleAccountsNoExplicitDefault() throws Exception {
        Settings.Builder builder = Settings.builder()
                .put("default_account", "account1");
        addAccountSettings("account1", builder);
        addAccountSettings("account2", builder);

        Accounts accounts = new Accounts(builder.build(), SecretService.Insecure.INSTANCE, logger);
        Account account = accounts.account("account1");
        assertThat(account, notNullValue());
        assertThat(account.name(), equalTo("account1"));
        account = accounts.account("account2");
        assertThat(account, notNullValue());
        assertThat(account.name(), equalTo("account2"));
        account = accounts.account(null);
        assertThat(account, notNullValue());
        assertThat(account.name(), isOneOf("account1", "account2"));
    }

    public void testMultipleAccountsUnknownDefault() throws Exception {
        Settings.Builder builder = Settings.builder()
                .put("default_account", "unknown");
        addAccountSettings("account1", builder);
        addAccountSettings("account2", builder);
        try {
            new Accounts(builder.build(), SecretService.Insecure.INSTANCE, logger);
            fail("Expected SettingsException");
        } catch (SettingsException e) {
            assertThat(e.getMessage(), is("could not find default email account [unknown]"));
        }
    }

    public void testNoAccount() throws Exception {
        Settings.Builder builder = Settings.builder();
        Accounts accounts = new Accounts(builder.build(), SecretService.Insecure.INSTANCE, logger);
        try {
            accounts.account(null);
            fail("no accounts are configured so trying to get the default account should throw an IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("cannot find default email account as no accounts have been configured"));
        }
    }

    public void testNoAccountWithDefaultAccount() throws Exception {
        Settings.Builder builder = Settings.builder()
                .put("default_account", "unknown");
        try {
            new Accounts(builder.build(), SecretService.Insecure.INSTANCE, logger);
            fail("Expected SettingsException");
        } catch (SettingsException e) {
            assertThat(e.getMessage(), is("could not find default email account [unknown]"));
        }
    }

    private void addAccountSettings(String name, Settings.Builder builder) {
        builder.put("account." + name + ".smtp.host", "_host");
    }
}
