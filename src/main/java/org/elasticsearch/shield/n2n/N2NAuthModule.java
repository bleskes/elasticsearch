package org.elasticsearch.shield.n2n;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.settings.Settings;

/**
 *
 */
public class N2NAuthModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(IPFilteringN2NAuthenticator.class).asEagerSingleton();
        bind(N2NNettyUpstreamHandler.class).asEagerSingleton();
    }
}
