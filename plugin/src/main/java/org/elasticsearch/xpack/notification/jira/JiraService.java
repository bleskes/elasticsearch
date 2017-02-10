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

package org.elasticsearch.xpack.notification.jira;

import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.common.http.HttpClient;
import org.elasticsearch.xpack.notification.NotificationService;

/**
 * A component to store Atlassian's JIRA credentials.
 *
 * https://www.atlassian.com/software/jira
 */
public class JiraService extends NotificationService<JiraAccount> {

    public static final Setting<Settings> JIRA_ACCOUNT_SETTING =
            Setting.groupSetting("xpack.notification.jira.", Setting.Property.Dynamic, Setting.Property.NodeScope);

    private final HttpClient httpClient;

    public JiraService(Settings settings, HttpClient httpClient, ClusterSettings clusterSettings) {
        super(settings);
        this.httpClient = httpClient;
        clusterSettings.addSettingsUpdateConsumer(JIRA_ACCOUNT_SETTING, this::setAccountSetting);
        setAccountSetting(JIRA_ACCOUNT_SETTING.get(settings));
    }

    @Override
    protected JiraAccount createAccount(String name, Settings accountSettings) {
        return new JiraAccount(name, accountSettings, httpClient);
    }
}
