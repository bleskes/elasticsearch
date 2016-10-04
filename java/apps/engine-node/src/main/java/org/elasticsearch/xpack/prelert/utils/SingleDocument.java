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

package org.elasticsearch.xpack.prelert.utils;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.StatusToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.RestStatus;

import java.io.IOException;

/**
 * Generic wrapper class for returning a single document requested through
 * the REST API. If the requested document does not exist {@link #isExists()}
 * will be false and {@link #getDocument()} will return <code>null</code>.
 */
public class SingleDocument implements StatusToXContent {
    private final boolean exists;
    private final String type;

    @Nullable
    private final BytesReference document;

    /**
     * Constructor for a SingleDocument with an existing doc
     *
     * @param type the document type
     * @param document the document (non-null)
     */
    public SingleDocument(String type, BytesReference document) {
        this.exists = document != null;
        this.type = type;
        this.document = document;
    }

    /**
     * Return true if the requested document exists
     * @return true is document exists
     */
    public boolean isExists() {
        return exists;
    }

    /**
     * The type of the requested document
     * @return The document type
     */
    public String getType() {
        return type;
    }

    /**
     * Get the requested document or null
     * @return The document or <code>null</code>
     */
    @Nullable
    public BytesReference getDocument() {
        return document;
    }

    @Override
    public RestStatus status() {
        return exists ? RestStatus.OK : RestStatus.NOT_FOUND;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.field("exists", exists);
        builder.field("type", type);
        if (document != null) {
            builder.rawField("document", document);
        }
        return builder;
    }

    /**
     * Creates an empty document with the given <code>type</code>
     * @param type the document type
     * @return The empty <code>SingleDocument</code>
     */
    public static SingleDocument empty(String type) {
        return new SingleDocument(type, null);
    }
}
