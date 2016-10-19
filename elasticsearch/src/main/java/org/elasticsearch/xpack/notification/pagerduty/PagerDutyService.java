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

import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.common.http.HttpClient;
import org.elasticsearch.xpack.notification.NotificationService;

/**
 * A component to store pagerduty credentials.
 */
public class PagerDutyService extends NotificationService<PagerDutyAccount> {

    public static final Setting<Settings> PAGERDUTY_ACCOUNT_SETTING =
        Setting.groupSetting("xpack.notification.pagerduty.", Setting.Property.Dynamic, Setting.Property.NodeScope);

    private final HttpClient httpClient;

    public PagerDutyService(Settings settings, HttpClient httpClient, ClusterSettings clusterSettings) {
        super(settings);
        this.httpClient = httpClient;
        clusterSettings.addSettingsUpdateConsumer(PAGERDUTY_ACCOUNT_SETTING, this::setAccountSetting);
        setAccountSetting(PAGERDUTY_ACCOUNT_SETTING.get(settings));
    }

    @Override
    protected PagerDutyAccount createAccount(String name, Settings accountSettings) {
        return new PagerDutyAccount(name, accountSettings, accountSettings, httpClient, logger);
    }

}
