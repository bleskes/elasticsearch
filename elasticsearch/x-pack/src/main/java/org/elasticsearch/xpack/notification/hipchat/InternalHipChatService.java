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

package org.elasticsearch.xpack.notification.hipchat;

import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.watcher.support.http.HttpClient;

/**
 *
 */
public class InternalHipChatService extends AbstractLifecycleComponent<HipChatService> implements HipChatService {

    private final HttpClient httpClient;
    private volatile HipChatAccounts accounts;
    public static final Setting<Settings> HIPCHAT_ACCOUNT_SETTING =
            Setting.groupSetting("xpack.notification.hipchat.service.", Setting.Property.Dynamic, Setting.Property.NodeScope);

    @Inject
    public InternalHipChatService(Settings settings, HttpClient httpClient, ClusterSettings clusterSettings) {
        super(settings);
        this.httpClient = httpClient;
        clusterSettings.addSettingsUpdateConsumer(HIPCHAT_ACCOUNT_SETTING, this::setHipchatAccountSetting);
    }

    @Override
    protected void doStart() {
        setHipchatAccountSetting(HIPCHAT_ACCOUNT_SETTING.get(settings));
    }

    @Override
    protected void doStop() {
    }

    @Override
    protected void doClose() {
    }

    private void setHipchatAccountSetting(Settings setting) {
        accounts = new HipChatAccounts(setting, httpClient, logger);
    }

    @Override
    public HipChatAccount getDefaultAccount() {
        return accounts.account(null);
    }

    @Override
    public HipChatAccount getAccount(String name) {
        return accounts.account(name);
    }
}
