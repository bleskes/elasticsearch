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

package org.elasticsearch.watcher.support.http;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.inject.SpawnModules;
import org.elasticsearch.watcher.support.http.auth.AuthModule;

import java.util.Collections;


/**
 */
public class HttpClientModule extends AbstractModule implements SpawnModules {

    @Override
    public Iterable<? extends Module> spawnModules() {
        return Collections.singletonList(new AuthModule());
    }

    @Override
    protected void configure() {
        bind(HttpRequestTemplate.Parser.class).asEagerSingleton();
        bind(HttpRequest.Parser.class).asEagerSingleton();
        bind(HttpClient.class).asEagerSingleton();
    }

}
