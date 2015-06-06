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

package org.elasticsearch.shield;

import com.google.common.collect.ImmutableList;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.action.ShieldActionModule;
import org.elasticsearch.shield.audit.AuditTrailModule;
import org.elasticsearch.shield.authc.AuthenticationModule;
import org.elasticsearch.shield.authz.AuthorizationModule;
import org.elasticsearch.shield.crypto.CryptoModule;
import org.elasticsearch.shield.license.LicenseModule;
import org.elasticsearch.shield.rest.ShieldRestModule;
import org.elasticsearch.shield.ssl.SSLModule;
import org.elasticsearch.shield.support.AbstractShieldModule;
import org.elasticsearch.shield.transport.ShieldTransportModule;

/**
 *
 */
public class ShieldModule extends AbstractShieldModule.Spawn {

    public ShieldModule(Settings settings) {
        super(settings);
    }

    @Override
    public Iterable<? extends Module> spawnModules(boolean clientMode) {
        assert shieldEnabled : "this module should get loaded only when shield is enabled";

        // spawn needed parts in client mode
        if (clientMode) {
            return ImmutableList.<Module>of(
                    new ShieldActionModule(settings),
                    new ShieldTransportModule(settings),
                    new SSLModule(settings));
        }

        return ImmutableList.<Module>of(
                new LicenseModule(settings),
                new CryptoModule(settings),
                new AuthenticationModule(settings),
                new AuthorizationModule(settings),
                new AuditTrailModule(settings),
                new ShieldRestModule(settings),
                new ShieldActionModule(settings),
                new ShieldTransportModule(settings),
                new SSLModule(settings));
    }

    @Override
    protected void configure(boolean clientMode) {
        if (!clientMode) {
            bind(ShieldSettingsFilter.class).asEagerSingleton();
        }
    }
}
