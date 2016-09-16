package org.elasticsearch.prelert;

import org.elasticsearch.common.inject.AbstractModule;

public class PrelertModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(PrelertServices.class).asEagerSingleton();
    }

}
