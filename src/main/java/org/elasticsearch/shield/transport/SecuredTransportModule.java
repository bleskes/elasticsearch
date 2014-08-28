package org.elasticsearch.shield.transport;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.shield.SecurityFilter;

/**
 *
 */
public class SecuredTransportModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(TransportFilter.class).to(SecurityFilter.Transport.class).asEagerSingleton();
    }
}
