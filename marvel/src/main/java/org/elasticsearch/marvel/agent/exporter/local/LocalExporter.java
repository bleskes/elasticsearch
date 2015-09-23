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

package org.elasticsearch.marvel.agent.exporter.local;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.marvel.agent.exporter.Exporter;
import org.elasticsearch.marvel.agent.exporter.MarvelDoc;
import org.elasticsearch.marvel.shield.SecuredClient;

import java.util.Collection;

/**
 *
 */
public class LocalExporter extends Exporter {

    public static final String TYPE = "local";

    private final Client client;

    public LocalExporter(Exporter.Config config, SecuredClient client) {
        super(TYPE, config);
        this.client = client;
    }

    @Override
    public void export(Collection<MarvelDoc> marvelDocs) {
    }

    @Override
    public void close() {
    }

    public static class Factory extends Exporter.Factory<LocalExporter> {

        private final SecuredClient client;

        @Inject
        public Factory(SecuredClient client) {
            super(TYPE, true);
            this.client = client;
        }

        @Override
        public LocalExporter create(Config config) {
            return new LocalExporter(config, client);
        }
    }
}
