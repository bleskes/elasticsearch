package org.elasticsearch.shield.transport.netty;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.transport.Transport;

/**
 *
 */
public class NettySecuredTransportModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Transport.class).to(NettySecuredTransport.class).asEagerSingleton();
    }
}
