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
import org.elasticsearch.xpack.watcher.support.http.HttpRequest;
import org.elasticsearch.xpack.watcher.support.http.HttpResponse;
import org.elasticsearch.xpack.watcher.watch.Payload;

import java.io.IOException;

/**
 *
 */
public class PagerDutyAccount {

    public static final String SERVICE_KEY_SETTING = "service_api_key";
    public static final String TRIGGER_DEFAULTS_SETTING = "event_defaults";

    final String name;
    final String serviceKey;
    final HttpClient httpClient;
    final IncidentEventDefaults eventDefaults;
    final ESLogger logger;

    public PagerDutyAccount(String name, Settings accountSettings, Settings serviceSettings, HttpClient httpClient, ESLogger logger) {
        this.name = name;
        this.serviceKey = accountSettings.get(SERVICE_KEY_SETTING, serviceSettings.get(SERVICE_KEY_SETTING, null));
        if (this.serviceKey == null) {
            throw new SettingsException("invalid pagerduty account [" + name + "]. missing required [" + SERVICE_KEY_SETTING + "] setting");
        }
        this.httpClient = httpClient;

        this.eventDefaults = new IncidentEventDefaults(accountSettings.getAsSettings(TRIGGER_DEFAULTS_SETTING));
        this.logger = logger;
    }

    public String getName() {
        return name;
    }

    public IncidentEventDefaults getDefaults() {
        return eventDefaults;
    }

    public SentEvent send(IncidentEvent event, Payload payload) throws IOException {
        HttpRequest request = event.createRequest(serviceKey, payload);
        HttpResponse response = httpClient.execute(request);
        return SentEvent.responded(event, request, response);
    }
}
