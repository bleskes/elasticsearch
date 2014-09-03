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
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.inject.PreProcessModule;
import org.elasticsearch.common.inject.SpawnModules;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.audit.AuditTrailModule;
import org.elasticsearch.shield.authc.AuthenticationModule;
import org.elasticsearch.shield.authz.AuthorizationModule;
import org.elasticsearch.shield.transport.SecuredTransportModule;

/**
 *
 */
public class SecurityModule extends AbstractModule implements SpawnModules, PreProcessModule {

    private final Settings settings;
    private final boolean isClient;
    private final boolean isShieldEnabled;

    public SecurityModule(Settings settings) {
        this.settings = settings;
        this.isClient = settings.getAsBoolean("node.client", false);
        this.isShieldEnabled = settings.getAsBoolean("shield.enabled", true);
    }

    @Override
    public void processModule(Module module) {
        if (module instanceof ActionModule && isShieldEnabled && !isClient) {
            ((ActionModule) module).registerFilter(SecurityFilter.Action.class);
        }
    }

    @Override
    public Iterable<? extends Module> spawnModules() {
        // don't spawn modules if shield is explicitly disabled
        if (!isShieldEnabled) {
            return ImmutableList.of();
        }

        // spawn needed parts in client mode
        if (isClient) {
            return ImmutableList.of(new SecuredTransportModule());
        }

        return ImmutableList.of(
                new AuthenticationModule(settings),
                new AuthorizationModule(),
                new AuditTrailModule(settings),
                new SecuredTransportModule());
    }

    @Override
    protected void configure() {
    }
}
