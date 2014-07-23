package org.elasticsearch.shield.n2n;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.settings.Settings;

/**
 *
 */
public class N2NAuthModule extends AbstractModule {

    private final Settings settings;

    public N2NAuthModule(Settings settings) {
        this.settings = settings;
    }

    @Override
    protected void configure() {
        bind(N2NNettyUpstreamHandler.class).asEagerSingleton();
    }
}
