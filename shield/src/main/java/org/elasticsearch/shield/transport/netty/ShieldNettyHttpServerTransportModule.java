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

package org.elasticsearch.shield.transport.netty;

import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.inject.PreProcessModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.http.HttpServerModule;
import org.elasticsearch.shield.ShieldPlugin;
import org.elasticsearch.shield.support.AbstractShieldModule;

/**
 *
 */
public class ShieldNettyHttpServerTransportModule extends AbstractShieldModule implements PreProcessModule {

    public ShieldNettyHttpServerTransportModule(Settings settings) {
        super(settings);
    }

    @Override
    public void processModule(Module module) {
        if (module instanceof HttpServerModule) {
            ((HttpServerModule) module).setHttpServerTransport(ShieldNettyHttpServerTransport.class, ShieldPlugin.NAME);
        }
    }

    @Override
    protected void configure(boolean clientMode) {
    }
}