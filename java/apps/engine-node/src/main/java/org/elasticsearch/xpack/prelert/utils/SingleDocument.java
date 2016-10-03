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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.elasticsearch.common.Nullable;

import java.util.Objects;

/**
 * Generic wrapper class for returning a single document requested through
 * the REST API. If the requested document does not exist {@link #isExists()}
 * will be false and {@link #getDocument()} will return <code>null</code>.
 *
 * @param <T> the requested document type
 */
@JsonPropertyOrder({"exists", "type", "document"})
@JsonInclude(Include.NON_NULL)
public class SingleDocument<T> {
    private final boolean exists;
    private final String type;

    @Nullable
    private final T document;

    /**
     * Constructor for a SingleDocument with an existing doc
     *
     * @param type the document type
     * @param document the document (non-null)
     */
    public SingleDocument(String type, T document) {
        this(true, type, Objects.requireNonNull(document));
    }

    private SingleDocument(boolean exists, String type, T document) {
        this.exists = exists;
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
    public T getDocument() {
        return document;
    }

    /**
     * Creates an empty document with the given <code>type</code>
     * @param type the document type
     * @return The empty <code>SingleDocument</code>
     */
    public static <T> SingleDocument<T> empty(String type) {
        return new SingleDocument<>(false, type, null);
    }
}
