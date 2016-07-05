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

package org.elasticsearch.xpack.security.crypto;

import org.elasticsearch.common.inject.util.Providers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xpack.security.support.AbstractSecurityModule;

/**
 *
 */
public class CryptoModule extends AbstractSecurityModule.Node {

    public CryptoModule(Settings settings) {
        super(settings);
    }

    @Override
    protected void configureNode() {
        if (securityEnabled == false) {
            bind(CryptoService.class).toProvider(Providers.of(null));
            return;
        }
        bind(InternalCryptoService.class).asEagerSingleton();
        bind(CryptoService.class).to(InternalCryptoService.class).asEagerSingleton();
    }
}
