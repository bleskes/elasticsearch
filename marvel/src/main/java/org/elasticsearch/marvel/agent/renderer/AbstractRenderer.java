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

package org.elasticsearch.marvel.agent.renderer;

import org.elasticsearch.common.util.CollectionUtils;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.marvel.agent.exporter.MarvelDoc;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Renders Marvel documents using a XContentBuilder (potentially filtered)
 */
public abstract class AbstractRenderer<T extends MarvelDoc> implements Renderer<T> {

    private static final String[] DEFAULT_FILTERS = {
            Fields.CLUSTER_NAME.underscore().toString(),
            Fields.TIMESTAMP.underscore().toString(),
    };

    private final String[] filters;

    public AbstractRenderer(String[] filters, boolean additive) {
        if (!additive) {
            this.filters = filters;
        } else {
            if (CollectionUtils.isEmpty(filters)) {
                this.filters = DEFAULT_FILTERS;
            } else {
                Set<String> additions = new HashSet<>();
                Collections.addAll(additions, DEFAULT_FILTERS);
                Collections.addAll(additions, filters);
                this.filters = additions.toArray(new String[additions.size()]);
            }
        }
    }

    @Override
    public void render(T marvelDoc, XContentType xContentType, OutputStream os) throws IOException {
        if (marvelDoc != null) {
            try (XContentBuilder builder = new XContentBuilder(xContentType.xContent(), os, filters())) {
                builder.startObject();

                // Add fields common to all Marvel documents
                builder.field(Fields.CLUSTER_NAME, marvelDoc.clusterName());
                DateTime timestampDateTime = new DateTime(marvelDoc.timestamp(), DateTimeZone.UTC);
                builder.field(Fields.TIMESTAMP, timestampDateTime.toString());

                // Render fields specific to the Marvel document
                doRender(marvelDoc, builder, ToXContent.EMPTY_PARAMS);

                builder.endObject();
            }
        }
    }

    protected abstract void doRender(T marvelDoc, XContentBuilder builder, ToXContent.Params params) throws IOException;

    /**
     * Returns the list of filters used when rendering the document. If null,
     * no filtering is applied.
     */
    protected String[] filters() {
        return filters;
    }

    static final class Fields {
        static final XContentBuilderString CLUSTER_NAME = new XContentBuilderString("cluster_name");
        static final XContentBuilderString TIMESTAMP = new XContentBuilderString("timestamp");
    }
}
