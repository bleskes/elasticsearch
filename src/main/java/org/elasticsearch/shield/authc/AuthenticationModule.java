package org.elasticsearch.shield.authc;

import org.elasticsearch.common.collect.ImmutableList;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.inject.Modules;
import org.elasticsearch.common.inject.SpawnModules;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.authc.esusers.ESUsersModule;
import org.elasticsearch.shield.authc.ldap.LdapModule;

/**
 *
 */
public class AuthenticationModule extends AbstractModule implements SpawnModules {

    private final Settings settings;

    public AuthenticationModule(Settings settings) {
        this.settings = settings;
    }

    @Override
    public Iterable<? extends Module> spawnModules() {
        ImmutableList.Builder<? extends Module> modules = ImmutableList.builder();
        modules.add(Modules.createModule(ESUsersModule.class, settings));
        if (LdapModule.enabled(settings)) {
            modules.add(Modules.createModule(LdapModule.class, settings));
        }
        return modules.build();
    }

    @Override
    protected void configure() {
        bind(AuthenticationService.class).to(InternalAuthenticationService.class);
    }
}
