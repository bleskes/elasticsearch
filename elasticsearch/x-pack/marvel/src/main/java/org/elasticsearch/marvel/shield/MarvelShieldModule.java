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
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.marvel.MarvelPlugin;

/**
 *
 */
public class MarvelShieldModule extends AbstractModule {

    private final boolean shieldEnabled;
    private final boolean marvelEnabled;

    public MarvelShieldModule(Settings settings) {
        this.shieldEnabled = MarvelShieldIntegration.enabled(settings);
        this.marvelEnabled = MarvelPlugin.marvelEnabled(settings);;
    }

    @Override
    protected void configure() {
        bind(MarvelShieldIntegration.class).asEagerSingleton();
        if (marvelEnabled) {
            bind(SecuredClient.class).asEagerSingleton();
        }
        if (shieldEnabled) {
            bind(MarvelSettingsFilter.Shield.class).asEagerSingleton();
            bind(MarvelSettingsFilter.class).to(MarvelSettingsFilter.Shield.class);
        } else {
            bind(MarvelSettingsFilter.class).toInstance(MarvelSettingsFilter.Noop.INSTANCE);
        }
    }
}
