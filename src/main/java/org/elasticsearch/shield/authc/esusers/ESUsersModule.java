package org.elasticsearch.shield.authc.esusers;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.shield.authc.support.UserPasswdStore;
import org.elasticsearch.shield.authc.support.UserRolesStore;

import static org.elasticsearch.common.inject.name.Names.named;

/**
 *
 */
public class ESUsersModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ESUsersRealm.class).asEagerSingleton();
        bind(UserPasswdStore.class).annotatedWith(named("file")).to(FileUserPasswdStore.class).asEagerSingleton();
        bind(UserRolesStore.class).annotatedWith(named("file")).to(FileUserRolesStore.class).asEagerSingleton();
    }
}
