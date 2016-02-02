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

package org.elasticsearch.watcher.shield;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.ShieldPlugin;
import org.elasticsearch.shield.ShieldSettingsFilter;

/**
 *
 */
public class ShieldIntegration {

    private final ShieldSettingsFilter settingsFilter;

    @Inject
    public ShieldIntegration(Settings settings, Injector injector) {
        boolean enabled = enabled(settings);
        settingsFilter = enabled ? injector.getInstance(ShieldSettingsFilter.class) : null;
    }

    public void filterOutSettings(String... patterns) {
        if (settingsFilter != null) {
            settingsFilter.filterOut(patterns);
        }
    }

    public static boolean enabled(Settings settings) {
        return ShieldPlugin.shieldEnabled(settings);
    }

}
