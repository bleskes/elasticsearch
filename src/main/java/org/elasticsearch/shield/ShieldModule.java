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

import org.elasticsearch.action.ActionModule;
import org.elasticsearch.common.collect.ImmutableList;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.inject.PreProcessModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.audit.AuditTrailModule;
import org.elasticsearch.shield.authc.AuthenticationModule;
import org.elasticsearch.shield.authz.AuthorizationModule;
import org.elasticsearch.shield.key.KeyModule;
import org.elasticsearch.shield.support.AbstractShieldModule;
import org.elasticsearch.shield.transport.SecuredRestModule;
import org.elasticsearch.shield.transport.SecuredTransportModule;

/**
 *
 */
public class ShieldModule extends AbstractShieldModule.Spawn implements PreProcessModule {

    private final boolean enabled;

    public ShieldModule(Settings settings) {
        super(settings);
        this.enabled = settings.getAsBoolean("shield.enabled", true);
    }

    @Override
    public void processModule(Module module) {
        if (module instanceof ActionModule && enabled && !clientMode) {
            ((ActionModule) module).registerFilter(SecurityFilter.Action.class);
        }
    }

    @Override
    public Iterable<? extends Module> spawnModules(boolean clientMode) {
        // don't spawn modules if shield is explicitly disabled
        if (!enabled) {
            return ImmutableList.of();
        }

        // spawn needed parts in client mode
        if (clientMode) {
            return ImmutableList.of(new SecuredTransportModule(settings));
        }

        return ImmutableList.of(
                new AuthenticationModule(settings),
                new AuthorizationModule(settings),
                new AuditTrailModule(settings),
                new SecuredTransportModule(settings),
                new SecuredRestModule(settings),
                new SecurityFilterModule(settings),
                new KeyModule(settings));
    }


    @Override
    protected void configure(boolean clientMode) {
    }
}
