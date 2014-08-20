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
import org.elasticsearch.shield.n2n.N2NModule;
import org.elasticsearch.shield.transport.SecuredTransportModule;
import org.elasticsearch.shield.transport.netty.NettySecuredHttpServerTransportModule;
import org.elasticsearch.shield.transport.netty.NettySecuredTransportModule;

/**
 *
 */
public class SecurityModule extends AbstractModule implements SpawnModules, PreProcessModule {

    private final Settings settings;

    public SecurityModule(Settings settings) {
        this.settings = settings;
    }

    @Override
    public void processModule(Module module) {
        if (module instanceof ActionModule) {
            ((ActionModule) module).registerFilter(SecurityFilter.Action.class);
        }
    }

    @Override
    public Iterable<? extends Module> spawnModules() {

        // don't spawn module in client mode
        if (settings.getAsBoolean("node.client", false)) {
            return ImmutableList.of();
        }

        // don't spawn modules if shield is explicitly disabled
        if (!settings.getComponentSettings(SecurityModule.class).getAsBoolean("enabled", true)) {
            return ImmutableList.of();
        }

        return ImmutableList.of(
                new AuthenticationModule(settings),
                new AuthorizationModule(),
                new AuditTrailModule(settings),
                new N2NModule(),
                new NettySecuredHttpServerTransportModule(),
                new NettySecuredTransportModule(),
                new SecuredTransportModule(settings));
    }

    @Override
    protected void configure() {
    }
}
