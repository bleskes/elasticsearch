package org.elasticsearch.shield.transport.netty;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.http.HttpServerTransport;

/**
 *
 */
public class NettySecuredHttpServerTransportModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(HttpServerTransport.class).to(NettySecuredHttpServerTransport.class).asEagerSingleton();
    }

}
