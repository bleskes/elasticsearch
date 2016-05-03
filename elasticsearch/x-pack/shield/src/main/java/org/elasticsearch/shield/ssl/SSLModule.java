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

package org.elasticsearch.shield.ssl;

import org.elasticsearch.common.inject.util.Providers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.ssl.SSLConfiguration.Global;
import org.elasticsearch.shield.support.AbstractShieldModule;

/**
 *
 */
public class SSLModule extends AbstractShieldModule {

    public SSLModule(Settings settings) {
        super(settings);
    }

    @Override
    protected void configure(boolean clientMode) {
        bind(Global.class).asEagerSingleton();
        bind(ClientSSLService.class).asEagerSingleton();
        if (clientMode) {
            bind(ServerSSLService.class).toProvider(Providers.<ServerSSLService>of(null));
        } else {
            bind(ServerSSLService.class).asEagerSingleton();
        }
    }
}
