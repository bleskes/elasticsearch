package org.elasticsearch.shield.n2n;

import org.elasticsearch.common.inject.AbstractModule;

/**
 *
 */
public class N2NModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(IPFilteringN2NAuthentricator.class).asEagerSingleton();
    }
}
