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

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.marvel.agent.exporter.ExportBulk;
import org.elasticsearch.marvel.agent.exporter.IndexNameResolver;
import org.elasticsearch.marvel.agent.exporter.MarvelDoc;
import org.elasticsearch.marvel.agent.renderer.Renderer;
import org.elasticsearch.marvel.agent.renderer.RendererRegistry;

import java.io.IOException;
import java.util.Collection;

/**
 *
 */
public class LocalBulk extends ExportBulk {

    private final Client client;
    private final IndexNameResolver indexNameResolver;
    private final RendererRegistry renderers;

    private BytesStreamOutput buffer = null;
    private BulkRequestBuilder requestBuilder;

    public LocalBulk(String name, Client client, IndexNameResolver indexNameResolver, RendererRegistry renderers) {
        super(name);
        this.client = client;
        this.indexNameResolver = indexNameResolver;
        this.renderers = renderers;
    }

    @Override
    public ExportBulk add(Collection<MarvelDoc> docs) throws Exception {

        for (MarvelDoc marvelDoc : docs) {
            if (requestBuilder == null) {
                requestBuilder = client.prepareBulk();
            }

            IndexRequestBuilder request = client.prepareIndex();
            if (marvelDoc.index() != null) {
                request.setIndex(marvelDoc.index());
            } else {
                request.setIndex(indexNameResolver.resolve(marvelDoc));
            }
            if (marvelDoc.type() != null) {
                request.setType(marvelDoc.type());
            }
            if (marvelDoc.id() != null) {
                request.setId(marvelDoc.id());
            }

            // Get the appropriate renderer in order to render the MarvelDoc
            Renderer renderer = renderers.renderer(marvelDoc.type());
            assert renderer != null : "unable to render marvel document of type [" + marvelDoc.type() + "]. no renderer found in registry";

            if (buffer == null) {
                buffer = new BytesStreamOutput();
            } else {
                buffer.reset();
            }

            renderer.render(marvelDoc, XContentType.SMILE, buffer);
            request.setSource(buffer.bytes().toBytes());

            requestBuilder.add(request);
        }
        return this;
    }

    @Override
    public void flush() throws IOException {
        if (requestBuilder == null) {
            return;
        }
        BulkResponse bulkResponse = requestBuilder.get();
        if (bulkResponse.hasFailures()) {
            throw new ElasticsearchException(bulkResponse.buildFailureMessage());
        }
    }

}
