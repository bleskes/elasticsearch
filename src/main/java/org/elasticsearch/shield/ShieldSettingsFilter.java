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

package org.elasticsearch.shield;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsFilter;

/**
 *
 */
public class ShieldSettingsFilter {

    static final String HIDE_SETTINGS_SETTING = "shield.hide_settings";

    private final SettingsFilter filter;

    @Inject
    public ShieldSettingsFilter(Settings settings, SettingsFilter settingsFilter) {
        this.filter = settingsFilter;
        filter.addFilter(HIDE_SETTINGS_SETTING);
        filterOut(settings.getAsArray(HIDE_SETTINGS_SETTING));
    }

    public void filterOut(String... patterns) {
        for (String pattern : patterns) {
            filter.addFilter(pattern);
        }
    }
}
