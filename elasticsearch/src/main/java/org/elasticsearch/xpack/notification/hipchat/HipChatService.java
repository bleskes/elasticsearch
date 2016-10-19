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

import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsException;
import org.elasticsearch.xpack.common.http.HttpClient;
import org.elasticsearch.xpack.notification.NotificationService;

/**
 * A component to store hipchat credentials.
 */
public class HipChatService extends NotificationService<HipChatAccount> {

    private final HttpClient httpClient;
    public static final Setting<Settings> HIPCHAT_ACCOUNT_SETTING =
        Setting.groupSetting("xpack.notification.hipchat.", Setting.Property.Dynamic, Setting.Property.NodeScope);
    private HipChatServer defaultServer;

    public HipChatService(Settings settings, HttpClient httpClient, ClusterSettings clusterSettings) {
        super(settings);
        this.httpClient = httpClient;
        clusterSettings.addSettingsUpdateConsumer(HIPCHAT_ACCOUNT_SETTING, this::setAccountSetting);
        setAccountSetting(HIPCHAT_ACCOUNT_SETTING.get(settings));
    }

    @Override
    protected synchronized void setAccountSetting(Settings settings) {
        defaultServer = new HipChatServer(settings);
        super.setAccountSetting(settings);
    }

    @Override
    protected HipChatAccount createAccount(String name, Settings accountSettings) {
        HipChatAccount.Profile profile = HipChatAccount.Profile.resolve(accountSettings, "profile", null);
        if (profile == null) {
            throw new SettingsException("missing [profile] setting for hipchat account [" + name + "]");
        }
        return profile.createAccount(name, accountSettings, defaultServer, httpClient, logger);
    }
}
