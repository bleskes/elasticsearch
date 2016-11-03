/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.prelert.utils;

import org.elasticsearch.action.support.ToXContentToBytes;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.StatusToXContent;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.RestStatus;

import java.io.IOException;
import java.util.Objects;

/**
 * Generic wrapper class for returning a single document requested through
 * the REST API. If the requested document does not exist {@link #isExists()}
 * will be false and {@link #getDocument()} will return <code>null</code>.
 */
public class SingleDocument<T extends ToXContent & Writeable> extends ToXContentToBytes implements Writeable, StatusToXContent {

    public static final ParseField DOCUMENT = new ParseField("document");
    public static final ParseField EXISTS = new ParseField("exists");
    public static final ParseField TYPE = new ParseField("type");

    private final boolean exists;
    private final String type;

    @Nullable
    private final T document;

    /**
     * Constructor for a SingleDocument with an existing doc
     *
     * @param type
     *            the document type
     * @param document
     *            the document (non-null)
     */
    public SingleDocument(String type, T document) {
        this.exists = document != null;
        this.type = type;
        this.document = document;
    }

    public SingleDocument(StreamInput in, Reader<T> documentReader) throws IOException {
        this.exists = in.readBoolean();
        this.type = in.readString();
        if (in.readBoolean()) {
            document = documentReader.read(in);
        } else {
            document = null;
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeBoolean(exists);
        out.writeString(type);
        boolean hasDocument = document != null;
        out.writeBoolean(hasDocument);
        if (hasDocument) {
            document.writeTo(out);
        }
    }

    /**
     * Return true if the requested document exists
     *
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
     *
     * @return The document or <code>null</code>
     */
    @Nullable
    public T getDocument() {
        return document;
    }

    @Override
    public RestStatus status() {
        return exists ? RestStatus.OK : RestStatus.NOT_FOUND;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.field(EXISTS.getPreferredName(), exists);
        builder.field(TYPE.getPreferredName(), type);
        if (document != null) {
            builder.field(DOCUMENT.getPreferredName(), document);
        }
        return builder;
    }

    /**
     * Creates an empty document with the given <code>type</code>
     * @param type the document type
     * @return The empty <code>SingleDocument</code>
     */
    public static <T extends ToXContent & Writeable> SingleDocument<T> empty(String type) {
        return new SingleDocument<T>(type, (T) null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(document, type, exists);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        @SuppressWarnings("unchecked")
        SingleDocument<T> other = (SingleDocument<T>) obj;
        return Objects.equals(exists, other.exists) &&
                Objects.equals(type, other.type) &&
                Objects.equals(document, other.document);
    }
}
