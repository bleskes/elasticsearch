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

package org.elasticsearch.watcher.support.secret;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.watcher.shield.ShieldIntegration;
import org.elasticsearch.watcher.shield.ShieldSecretService;

/**
 *
 */
public class SecretModule extends AbstractModule {

    private final boolean shieldEnabled;

    public SecretModule(Settings settings) {
        shieldEnabled = ShieldIntegration.enabled(settings);
    }

    @Override
    protected void configure() {
        if (shieldEnabled) {
            bind(ShieldSecretService.class).asEagerSingleton();
            bind(SecretService.class).to(ShieldSecretService.class);
        } else {
            bind(SecretService.PlainText.class).asEagerSingleton();
            bind(SecretService.class).to(SecretService.PlainText.class);
        }
    }
}
