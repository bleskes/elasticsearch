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

import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

/**
 *
 */
public class AccountsTests extends ElasticsearchTestCase {

    @Test
    public void testSingleAccount() throws Exception {
        ImmutableSettings.Builder builder = ImmutableSettings.builder()
                .put("default_account", "account1");
        addAccountSettings("account1", builder);

        Accounts accounts = new Accounts(builder.build(), logger);
        Account account = accounts.account("account1");
        assertThat(account, notNullValue());
        assertThat(account.name(), equalTo("account1"));
        account = accounts.account(null); // falling back on the default
        assertThat(account, notNullValue());
        assertThat(account.name(), equalTo("account1"));
    }

    @Test
    public void testSingleAccount_NoExplicitDefault() throws Exception {
        ImmutableSettings.Builder builder = ImmutableSettings.builder();
        addAccountSettings("account1", builder);

        Accounts accounts = new Accounts(builder.build(), logger);
        Account account = accounts.account("account1");
        assertThat(account, notNullValue());
        assertThat(account.name(), equalTo("account1"));
        account = accounts.account(null); // falling back on the default
        assertThat(account, notNullValue());
        assertThat(account.name(), equalTo("account1"));
    }

    @Test
    public void testMultipleAccounts() throws Exception {
        ImmutableSettings.Builder builder = ImmutableSettings.builder()
                .put("default_account", "account1");
        addAccountSettings("account1", builder);
        addAccountSettings("account2", builder);

        Accounts accounts = new Accounts(builder.build(), logger);
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

    @Test
    public void testMultipleAccounts_NoExplicitDefault() throws Exception {
        ImmutableSettings.Builder builder = ImmutableSettings.builder()
                .put("default_account", "account1");
        addAccountSettings("account1", builder);
        addAccountSettings("account2", builder);

        Accounts accounts = new Accounts(builder.build(), logger);
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

    @Test(expected = EmailSettingsException.class)
    public void testMultipleAccounts_UnknownDefault() throws Exception {
        ImmutableSettings.Builder builder = ImmutableSettings.builder()
                .put("default_account", "unknown");
        addAccountSettings("account1", builder);
        addAccountSettings("account2", builder);
        new Accounts(builder.build(), logger);
    }

    @Test
    public void testNoAccount() throws Exception {
        ImmutableSettings.Builder builder = ImmutableSettings.builder();
        Accounts accounts = new Accounts(builder.build(), logger);
        Account account = accounts.account(null);
        assertThat(account, nullValue());
    }

    @Test(expected = EmailSettingsException.class)
    public void testNoAccount_WithDefaultAccount() throws Exception {
        ImmutableSettings.Builder builder = ImmutableSettings.builder()
                .put("default_account", "unknown");
        new Accounts(builder.build(), logger);
    }

    private void addAccountSettings(String name, ImmutableSettings.Builder builder) {
        builder.put("account." + name + ".smtp.host", "_host");
    }
}
