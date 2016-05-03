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

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsException;
import org.elasticsearch.xpack.watcher.support.http.HttpClient;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class PagerDutyAccounts {

    private final Map<String, PagerDutyAccount> accounts;
    private final String defaultAccountName;

    public PagerDutyAccounts(Settings serviceSettings, HttpClient httpClient, ESLogger logger) {
        Settings accountsSettings = serviceSettings.getAsSettings("account");
        accounts = new HashMap<>();
        for (String name : accountsSettings.names()) {
            Settings accountSettings = accountsSettings.getAsSettings(name);
            PagerDutyAccount account = new PagerDutyAccount(name, accountSettings, serviceSettings, httpClient, logger);
            accounts.put(name, account);
        }

        String defaultAccountName = serviceSettings.get("default_account");
        if (defaultAccountName == null) {
            if (accounts.isEmpty()) {
                this.defaultAccountName = null;
            } else {
                PagerDutyAccount account = accounts.values().iterator().next();
                logger.info("default pager duty account set to [{}]", account.name);
                this.defaultAccountName = account.name;
            }
        } else if (!accounts.containsKey(defaultAccountName)) {
            throw new SettingsException("could not find default pagerduty account [" + defaultAccountName + "]");
        } else {
            this.defaultAccountName = defaultAccountName;
        }
    }

    /**
     * Returns the account associated with the given name. If there is not such account, {@code null} is returned.
     * If the given name is {@code null}, the default account will be returned.
     *
     * @param name  The name of the requested account
     * @return      The account associated with the given name, or {@code null} when requested an unknown account.
     * @throws      IllegalStateException if the name is null and the default account is null.
     */
    public PagerDutyAccount account(String name) throws IllegalStateException {
        if (name == null) {
            if (defaultAccountName == null) {
                throw new IllegalStateException("cannot find default pagerduty account as no accounts have been configured");
            }
            name = defaultAccountName;
        }
        return accounts.get(name);
    }
}
