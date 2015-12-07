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

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.HasContext;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.ShieldPlugin;
import org.elasticsearch.shield.ShieldSettingsFilter;
import org.elasticsearch.shield.authc.AuthenticationService;
import org.elasticsearch.transport.TransportMessage;

import java.io.IOException;

/**
 *
 */
public class ShieldIntegration {

    private final boolean enabled;
    private final AuthenticationService authcService;
    private final ShieldSettingsFilter settingsFilter;

    @Inject
    public ShieldIntegration(Settings settings, Injector injector) {
        enabled = enabled(settings);
        authcService = enabled ? injector.getInstance(AuthenticationService.class) : null;
        settingsFilter = enabled ? injector.getInstance(ShieldSettingsFilter.class) : null;
    }

    public void bindWatcherUser(TransportMessage message) {
        if (authcService != null) {
            try {
                authcService.attachUserHeaderIfMissing(message, InternalWatcherUser.INSTANCE);
            } catch (IOException e) {
                throw new ElasticsearchException("failed to attach watcher user to request", e);
            }
        }
    }

    public void filterOutSettings(String... patterns) {
        if (settingsFilter != null) {
            settingsFilter.filterOut(patterns);
        }
    }

    // TODO this is a hack that needs to go away with proper fixes in core
    public void putUserInContext(HasContext context) {
        if (enabled) {
            context.putInContext("_shield_user", InternalWatcherUser.INSTANCE);
        }
    }

    public static boolean enabled(Settings settings) {
        return ShieldPlugin.shieldEnabled(settings);
    }

}
