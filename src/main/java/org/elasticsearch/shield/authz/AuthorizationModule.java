package org.elasticsearch.shield.authz;

import org.elasticsearch.common.inject.AbstractModule;

/**
 *
 */
public class AuthorizationModule extends AbstractModule {

    @Override
    protected void configure() {

        bind(RolesStore.class).to(FileRolesStore.class);
        bind(AuthorizationService.class).to(InternalAuthorizationService.class);
    }
}
