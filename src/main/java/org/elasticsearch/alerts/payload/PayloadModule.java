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

package org.elasticsearch.alerts.payload;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.multibindings.MapBinder;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class PayloadModule extends AbstractModule {

    private Map<String, Class<? extends Payload.Parser>> parsers = new HashMap<>();

    public void registerPayload(String payloadType, Class<? extends Payload.Parser> parserType) {
        parsers.put(payloadType, parserType);
    }

    @Override
    protected void configure() {

        MapBinder<String, Payload.Parser> mbinder = MapBinder.newMapBinder(binder(), String.class, Payload.Parser.class);
        bind(SearchPayload.Parser.class).asEagerSingleton();
        mbinder.addBinding(SearchPayload.TYPE).to(SearchPayload.Parser.class);

        for (Map.Entry<String, Class<? extends Payload.Parser>> entry : parsers.entrySet()) {
            bind(entry.getValue()).asEagerSingleton();
            mbinder.addBinding(entry.getKey()).to(entry.getValue());
        }
    }
}
