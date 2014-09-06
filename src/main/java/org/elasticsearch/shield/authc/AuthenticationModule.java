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

package org.elasticsearch.shield.authc;

import org.elasticsearch.common.collect.ImmutableList;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.inject.SpawnModules;
import org.elasticsearch.common.inject.util.Providers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.authc.esusers.ESUsersModule;
import org.elasticsearch.shield.authc.esusers.ESUsersRealm;
import org.elasticsearch.shield.authc.ldap.LdapModule;
import org.elasticsearch.shield.authc.ldap.LdapRealm;
import org.elasticsearch.shield.authc.system.SystemRealm;

import static org.elasticsearch.common.inject.name.Names.named;

/**
 *
 */
public class AuthenticationModule extends AbstractModule implements SpawnModules {

    private final Settings settings;
    private final boolean esusersEnabled;
    private final boolean ldapEnabled;

    public AuthenticationModule(Settings settings) {
        this.settings = settings;
        this.esusersEnabled = ESUsersModule.enabled(settings);
        this.ldapEnabled = LdapModule.enabled(settings);
    }

    @Override
    public Iterable<? extends Module> spawnModules() {
        ImmutableList.Builder<Module> modules = ImmutableList.builder();
        modules.add(new SystemRealm.Module());
        if (esusersEnabled) {
            modules.add(new ESUsersModule());
        }
        if (ldapEnabled) {
            modules.add(new LdapModule(settings));
        }
        return modules.build();
    }

    @Override
    protected void configure() {
        bind(AuthenticationService.class).to(InternalAuthenticationService.class).asEagerSingleton();
        if (!esusersEnabled) {
            bind(ESUsersRealm.class).toProvider(Providers.of((ESUsersRealm) null));
        }
        if (!ldapEnabled) {
            bind(LdapRealm.class).toProvider(Providers.of((LdapRealm) null));
        }
    }
}
