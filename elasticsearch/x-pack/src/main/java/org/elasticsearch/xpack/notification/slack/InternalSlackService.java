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

package org.elasticsearch.xpack.notification.slack;

import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.watcher.support.http.HttpClient;

/**
 *
 */
public class InternalSlackService extends AbstractLifecycleComponent<SlackService> implements SlackService {

    private final HttpClient httpClient;
    public static final Setting<Settings> SLACK_ACCOUNT_SETTING =
            Setting.groupSetting("xpack.notification.slack.service.", Setting.Property.Dynamic, Setting.Property.NodeScope);
    private volatile SlackAccounts accounts;

    @Inject
    public InternalSlackService(Settings settings, HttpClient httpClient, ClusterSettings clusterSettings) {
        super(settings);
        this.httpClient = httpClient;
        clusterSettings.addSettingsUpdateConsumer(SLACK_ACCOUNT_SETTING, this::setSlackAccountSetting);
    }

    @Override
    protected void doStart() {
        setSlackAccountSetting(SLACK_ACCOUNT_SETTING.get(settings));
    }

    @Override
    protected void doStop() {
    }

    @Override
    protected void doClose() {
    }

    @Override
    public SlackAccount getDefaultAccount() {
        return accounts.account(null);
    }

    private void setSlackAccountSetting(Settings setting) {
        accounts = new SlackAccounts(setting, httpClient, logger);
    }

    @Override
    public SlackAccount getAccount(String name) {
        return accounts.account(name);
    }
}
