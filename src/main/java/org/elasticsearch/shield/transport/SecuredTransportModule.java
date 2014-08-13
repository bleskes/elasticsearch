package org.elasticsearch.shield.transport;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.SecurityFilter;

/**
 *
 */
public class SecuredTransportModule extends AbstractModule {

    private final Settings settings;

    public SecuredTransportModule(Settings settings) {
        this.settings = settings;
    }

    @Override
    protected void configure() {
        if (!settings.getAsBoolean("node.client", false)) {
            bind(TransportFilter.class).to(SecurityFilter.Transport.class).asEagerSingleton();
        }
    }
}
