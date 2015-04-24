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

import org.elasticsearch.ElasticsearchIllegalStateException;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.ShieldPlugin;
import org.elasticsearch.shield.ShieldSettingsFilter;
import org.elasticsearch.shield.ShieldVersion;
import org.elasticsearch.shield.authc.AuthenticationService;
import org.elasticsearch.transport.TransportMessage;
import org.elasticsearch.watcher.WatcherVersion;

/**
 *
 */
public class ShieldIntegration {

    private static final int minCompatibleShieldVersionId = /*00*/1020199; // V_1_2_1

    private final boolean installed;
    private final boolean enabled;
    private final Object authcService;
    private final Object userHolder;
    private final Object settingsFilter;

    @Inject
    public ShieldIntegration(Settings settings, Injector injector) {
        installed = installed(settings);
        enabled = installed && ShieldPlugin.shieldEnabled(settings);
        authcService = enabled ? injector.getInstance(AuthenticationService.class) : null;
        userHolder = enabled ? injector.getInstance(WatcherUserHolder.class) : null;
        settingsFilter = enabled ? injector.getInstance(ShieldSettingsFilter.class) : null;

    }

    public boolean installed() {
        return installed;
    }

    public boolean enabled() {
        return enabled;
    }

    public void bindWatcherUser(TransportMessage message) {
        if (authcService != null) {
            ((AuthenticationService) authcService).attachUserHeaderIfMissing(message, ((WatcherUserHolder) userHolder).user);
        }
    }

    public void filterOutSettings(String... patterns) {
        if (settingsFilter != null) {
            ((ShieldSettingsFilter) settingsFilter).filterOut(patterns);
        }
    }

    static boolean installed(Settings settings) {
        try {
            Class clazz = settings.getClassLoader().loadClass("org.elasticsearch.shield.ShieldPlugin");
            if (clazz == null) {
                return false;
            }

            // lets check min compatibility
            ShieldVersion minVersion = ShieldVersion.fromId(minCompatibleShieldVersionId);
            if (!ShieldVersion.CURRENT.onOrAfter(minVersion)) {
                throw new ElasticsearchIllegalStateException("watcher [" + WatcherVersion.CURRENT + "] requires " +
                        "minimum shield plugin version [" + minVersion + "], but installed shield plugin version is " +
                        "[" + ShieldVersion.CURRENT + "]");
            }

            return true;

        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean enabled(Settings settings) {
        return installed(settings) && ShieldPlugin.shieldEnabled(settings);
    }

}
