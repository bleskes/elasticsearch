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

package org.elasticsearch.xpack.common.http;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.multibindings.MapBinder;
import org.elasticsearch.xpack.common.http.auth.basic.BasicAuth;
import org.elasticsearch.xpack.common.http.auth.basic.BasicAuthFactory;
import org.elasticsearch.xpack.common.http.auth.HttpAuthFactory;
import org.elasticsearch.xpack.common.http.auth.HttpAuthRegistry;


/**
 */
public class HttpClientModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(HttpRequestTemplate.Parser.class).asEagerSingleton();
        bind(HttpRequest.Parser.class).asEagerSingleton();
        bind(HttpClient.class).asEagerSingleton();

        MapBinder<String, HttpAuthFactory> parsersBinder = MapBinder.newMapBinder(binder(), String.class, HttpAuthFactory.class);

        bind(BasicAuthFactory.class).asEagerSingleton();
        parsersBinder.addBinding(BasicAuth.TYPE).to(BasicAuthFactory.class);

        bind(HttpAuthRegistry.class).asEagerSingleton();
    }

}
