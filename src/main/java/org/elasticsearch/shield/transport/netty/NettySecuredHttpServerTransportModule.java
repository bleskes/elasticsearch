package org.elasticsearch.shield.transport.netty;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.inject.PreProcessModule;
import org.elasticsearch.http.HttpServerModule;
import org.elasticsearch.shield.plugin.SecurityPlugin;

/**
 *
 */
public class NettySecuredHttpServerTransportModule extends AbstractModule implements PreProcessModule {

    @Override
    public void processModule(Module module) {
        if (module instanceof HttpServerModule) {
            ((HttpServerModule)module).setHttpServerTransport(NettySecuredHttpServerTransport.class, SecurityPlugin.NAME);
        }
    }

    @Override
    protected void configure() {}
}