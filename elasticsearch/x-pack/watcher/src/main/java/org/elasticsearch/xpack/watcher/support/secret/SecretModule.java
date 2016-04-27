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

package org.elasticsearch.xpack.watcher.support.secret;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.Security;

/**
 *
 */
public class SecretModule extends AbstractModule {

    private final boolean securityEnabled;

    public SecretModule(Settings settings) {
        securityEnabled = Security.enabled(settings);
    }

    @Override
    protected void configure() {
        if (securityEnabled) {
            bind(SecretService.Secure.class).asEagerSingleton();
            bind(SecretService.class).to(SecretService.Secure.class);
        } else {
            bind(SecretService.class).toInstance(SecretService.Insecure.INSTANCE);
        }
    }
}
