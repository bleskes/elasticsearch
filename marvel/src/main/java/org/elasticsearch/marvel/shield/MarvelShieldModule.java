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

package org.elasticsearch.marvel.shield;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.util.Providers;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.authz.Privilege;

/**
 *
 */
public class MarvelShieldModule extends AbstractModule {

    private final MarvelInternalUserHolder userHolder;
    private final boolean enabled;

    public MarvelShieldModule(Settings settings) {
        this.enabled = MarvelShieldIntegration.enabled(settings);
        userHolder = enabled ? new MarvelInternalUserHolder() : null;
    }

    @Override
    protected void configure() {
        bind(MarvelShieldIntegration.class).asEagerSingleton();
        bind(SecuredClient.class).asEagerSingleton();
        bind(MarvelInternalUserHolder.class).toProvider(Providers.of(userHolder));
        if (enabled) {
            bind(MarvelSettingsFilter.Shield.class).asEagerSingleton();
            bind(MarvelSettingsFilter.class).to(MarvelSettingsFilter.Shield.class);
        } else {
            bind(MarvelSettingsFilter.class).toInstance(MarvelSettingsFilter.Noop.INSTANCE);
        }
    }
}
