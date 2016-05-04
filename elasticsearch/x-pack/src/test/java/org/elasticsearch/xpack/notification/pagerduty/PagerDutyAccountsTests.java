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

package org.elasticsearch.xpack.notification.pagerduty;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsException;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.common.http.HttpClient;
import org.elasticsearch.xpack.notification.slack.message.SlackMessageDefaultsTests;
import org.junit.Before;

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;

/**
 *
 */
public class PagerDutyAccountsTests extends ESTestCase {

    private HttpClient httpClient;

    @Before
    public void init() throws Exception {
        httpClient = mock(HttpClient.class);
    }

    public void testSingleAccount() throws Exception {
        Settings.Builder builder = Settings.builder()
                .put("default_account", "account1");
        addAccountSettings("account1", builder);

        PagerDutyAccounts accounts = new PagerDutyAccounts(builder.build(), httpClient, logger);
        PagerDutyAccount account = accounts.account("account1");
        assertThat(account, notNullValue());
        assertThat(account.name, equalTo("account1"));
        account = accounts.account(null); // falling back on the default
        assertThat(account, notNullValue());
        assertThat(account.name, equalTo("account1"));
    }

    public void testSingleAccountNoExplicitDefault() throws Exception {
        Settings.Builder builder = Settings.builder();
        addAccountSettings("account1", builder);

        PagerDutyAccounts accounts = new PagerDutyAccounts(builder.build(), httpClient, logger);
        PagerDutyAccount account = accounts.account("account1");
        assertThat(account, notNullValue());
        assertThat(account.name, equalTo("account1"));
        account = accounts.account(null); // falling back on the default
        assertThat(account, notNullValue());
        assertThat(account.name, equalTo("account1"));
    }

    public void testMultipleAccounts() throws Exception {
        Settings.Builder builder = Settings.builder()
                .put("default_account", "account1");
        addAccountSettings("account1", builder);
        addAccountSettings("account2", builder);

        PagerDutyAccounts accounts = new PagerDutyAccounts(builder.build(), httpClient, logger);
        PagerDutyAccount account = accounts.account("account1");
        assertThat(account, notNullValue());
        assertThat(account.name, equalTo("account1"));
        account = accounts.account("account2");
        assertThat(account, notNullValue());
        assertThat(account.name, equalTo("account2"));
        account = accounts.account(null); // falling back on the default
        assertThat(account, notNullValue());
        assertThat(account.name, equalTo("account1"));
    }

    public void testMultipleAccounts_NoExplicitDefault() throws Exception {
        Settings.Builder builder = Settings.builder()
                .put("default_account", "account1");
        addAccountSettings("account1", builder);
        addAccountSettings("account2", builder);

        PagerDutyAccounts accounts = new PagerDutyAccounts(builder.build(), httpClient, logger);
        PagerDutyAccount account = accounts.account("account1");
        assertThat(account, notNullValue());
        assertThat(account.name, equalTo("account1"));
        account = accounts.account("account2");
        assertThat(account, notNullValue());
        assertThat(account.name, equalTo("account2"));
        account = accounts.account(null);
        assertThat(account, notNullValue());
        assertThat(account.name, isOneOf("account1", "account2"));
    }

    public void testMultipleAccounts_UnknownDefault() throws Exception {
        try {
            Settings.Builder builder = Settings.builder()
                    .put("default_account", "unknown");
            addAccountSettings("account1", builder);
            addAccountSettings("account2", builder);
            new PagerDutyAccounts(builder.build(), httpClient, logger);
            fail("Expected a SettingsException to happen");
        } catch (SettingsException e) {}
    }

    public void testNoAccount() throws Exception {
        try {
            Settings.Builder builder = Settings.builder();
            PagerDutyAccounts accounts = new PagerDutyAccounts(builder.build(), httpClient, logger);
            accounts.account(null);
            fail("no accounts are configured so trying to get the default account should throw an IllegalStateException");
        } catch (IllegalStateException e) {}
    }

    public void testNoAccount_WithDefaultAccount() throws Exception {
        try {
            Settings.Builder builder = Settings.builder()
                    .put("default_account", "unknown");
            new PagerDutyAccounts(builder.build(), httpClient, logger);
            fail("Expected a SettingsException to happen");
        } catch (SettingsException e) {}
    }

    private void addAccountSettings(String name, Settings.Builder builder) {
        builder.put("account." + name + ".service_api_key", randomAsciiOfLength(50));
        Settings defaults = SlackMessageDefaultsTests.randomSettings();
        for (Map.Entry<String, String> setting : defaults.getAsMap().entrySet()) {
            builder.put("message_defaults." + setting.getKey(), setting.getValue());
        }
    }
}
