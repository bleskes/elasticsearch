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

import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.watcher.support.http.HttpClient;

/**
 *
 */
public class InternalPagerDutyService extends AbstractLifecycleComponent<PagerDutyService> implements PagerDutyService {

    public static final Setting<Settings> PAGERDUTY_ACCOUNT_SETTING =
            Setting.groupSetting("xpack.notification.pagerduty.service.", Setting.Property.Dynamic, Setting.Property.NodeScope);

    private final HttpClient httpClient;
    private volatile PagerDutyAccounts accounts;

    @Inject
    public InternalPagerDutyService(Settings settings, HttpClient httpClient, ClusterSettings clusterSettings) {
        super(settings);
        this.httpClient = httpClient;
        clusterSettings.addSettingsUpdateConsumer(PAGERDUTY_ACCOUNT_SETTING, this::setPagerDutyAccountSetting);
    }

    @Override
    protected void doStart() {
        setPagerDutyAccountSetting(PAGERDUTY_ACCOUNT_SETTING.get(settings));
    }

    @Override
    protected void doStop() {
    }

    @Override
    protected void doClose() {
    }

    private void setPagerDutyAccountSetting(Settings settings) {
        accounts = new PagerDutyAccounts(settings, httpClient, logger);
    }

    @Override
    public PagerDutyAccount getDefaultAccount() {
        return accounts.account(null);
    }

    @Override
    public PagerDutyAccount getAccount(String name) {
        return accounts.account(name);
    }
}
