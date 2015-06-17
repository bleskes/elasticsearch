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

package org.elasticsearch.watcher.support.xcontent;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates the xcontent source
 */
public class XContentSource implements ToXContent {

    private final BytesReference bytes;
    private final XContentType contentType;
    private Object data;

    /**
     * Constructs a new XContentSource out of the given bytes reference.
     */
    public XContentSource(BytesReference bytes, XContentType xContentType) throws ElasticsearchParseException {
        if (xContentType == null) {
            throw new IllegalArgumentException("xContentType must not be null");
        }
        this.bytes = bytes;
        this.contentType = xContentType;
    }

    public XContentSource(BytesReference bytes) {
        this(bytes, XContentFactory.xContentType(bytes));
    }

    /**
     * @return The bytes reference of the source
     */
    public BytesReference getBytes() {
        return bytes;
    }

    /**
     * @return true if the top level value of the source is a map
     */
    public boolean isMap() {
        return data() instanceof Map;
    }

    /**
     * @return The source as a map
     */
    public Map<String, Object> getAsMap() {
        return (Map<String, Object>) data();
    }

    /**
     * @return true if the top level value of the source is a list
     */
    public boolean isList() {
        return data() instanceof List;
    }

    /**
     * @return The source as a list
     */
    public List<Object> getAsList() {
        return (List<Object>) data();
    }

    /**
     * Extracts a value identified by the given path in the source.
     *
     * @param path a dot notation path to the requested value
     * @return The extracted value or {@code null} if no value is associated with the given path
     */
    public <T> T getValue(String path) {
        return (T) ObjectPath.eval(path, data());
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        try (XContentParser parser = parser()) {
            parser.nextToken();
            XContentHelper.copyCurrentStructure(builder.generator(), parser);
            return builder;
        }
    }

    public XContentParser parser() throws IOException {
        return contentType.xContent().createParser(bytes);
    }

    public static XContentSource readFrom(StreamInput in) throws IOException {
        return new XContentSource(in.readBytesReference(), XContentType.readFrom(in));
    }

    public static void writeTo(XContentSource source, StreamOutput out) throws IOException {
        out.writeBytesReference(source.bytes);
        XContentType.writeTo(source.contentType, out);
    }

    private Object data() {
        if (data == null) {
            try (XContentParser parser = parser()) {
                data = WatcherXContentUtils.readValue(parser, parser.nextToken());
            } catch (IOException ex) {
                throw new ElasticsearchException("failed to read value", ex);
            }
        }
        return data;
    }

}
