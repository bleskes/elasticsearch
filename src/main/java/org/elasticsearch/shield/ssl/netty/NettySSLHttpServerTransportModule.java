package org.elasticsearch.shield.ssl.netty;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.http.HttpServerTransport;
import org.elasticsearch.http.netty.NettyHttpServerTransport;

/**
 *
 */
public class NettySSLHttpServerTransportModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(HttpServerTransport.class).to(NettySSLHttpServerTransport.class).asEagerSingleton();
    }

}
