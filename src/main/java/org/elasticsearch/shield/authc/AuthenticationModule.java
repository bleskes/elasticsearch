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
        if (ESUsersModule.enabled(settings)) {
            modules.add(new ESUsersModule());
        }
        if (LdapModule.enabled(settings)) {
            modules.add(new LdapModule());
        }
        return modules.build();
    }

    @Override
    protected void configure() {
        bind(AuthenticationService.class).to(InternalAuthenticationService.class);
    }
}
