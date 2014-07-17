package org.elasticsearch.shield.ssl.netty;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.transport.Transport;

/**
 *
 */
public class NettySSLTransportModule extends AbstractModule {

    private final Settings settings;

    public NettySSLTransportModule(Settings settings) {
        this.settings = settings;
    }

    @Override
    protected void configure() {
        bind(NettySSLTransport.class).asEagerSingleton();
        bind(Transport.class).to(NettySSLTransport.class);
    }
}
