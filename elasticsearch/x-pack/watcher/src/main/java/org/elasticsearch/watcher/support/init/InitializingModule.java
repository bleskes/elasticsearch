/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */

package org.elasticsearch.watcher.support.init;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.multibindings.Multibinder;
import org.elasticsearch.watcher.input.chain.ChainInputFactory;
import org.elasticsearch.watcher.support.init.proxy.ClientProxy;
import org.elasticsearch.watcher.support.init.proxy.ScriptServiceProxy;
import org.elasticsearch.watcher.transform.chain.ChainTransformFactory;

/**
 *
 */
public class InitializingModule extends AbstractModule {

    @Override
    protected void configure() {

        bind(ClientProxy.class).asEagerSingleton();
        bind(ScriptServiceProxy.class).asEagerSingleton();
        bind(ChainInputFactory.class).asEagerSingleton();

        Multibinder<InitializingService.Initializable> mbinder = Multibinder.newSetBinder(binder(), InitializingService.Initializable.class);
        mbinder.addBinding().to(ClientProxy.class);
        mbinder.addBinding().to(ScriptServiceProxy.class);
        mbinder.addBinding().to(ChainTransformFactory.class);
        mbinder.addBinding().to(ChainInputFactory.class);
        bind(InitializingService.class).asEagerSingleton();
    }
}
