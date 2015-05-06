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

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.*;
import org.elasticsearch.common.xcontent.support.XContentMapValues;

import java.io.IOException;
import java.util.Map;

/**
 * Encapsulates the xcontent source
 */
public class XContentSource implements ToXContent {

    private final BytesReference bytes;

    private XContentType contentType;
    private Map<String, Object> data;

    /**
     * Constructs a new XContentSource out of the given bytes reference.
     */
    public XContentSource(BytesReference bytes) throws ElasticsearchParseException {
        this.bytes = bytes;
    }

    /**
     * @return The bytes reference of the source
     */
    public BytesReference getBytes() {
        return bytes;
    }

    /**
     * @return The source as a map
     */
    public Map<String, Object> getAsMap() {
        if (data == null) {
            Tuple<XContentType, Map<String, Object>> tuple = XContentHelper.convertToMap(bytes, false);
            this.contentType = tuple.v1();
            this.data = tuple.v2();
        }
        return data;
    }

    /**
     * Extracts a value identified by the given path in the source.
     *
     * @param path a dot notation path to the requested value
     * @return The extracted value or {@code null} if no value is associated with the given path
     */
    public <T> T getValue(String path) {
        return (T) XContentMapValues.extractValue(path, getAsMap());
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        XContentParser parser = contentType().xContent().createParser(bytes);
        parser.nextToken();
        XContentHelper.copyCurrentStructure(builder.generator(), parser);
        return builder;
    }

    public static XContentSource readFrom(StreamInput in) throws IOException {
        return new XContentSource(in.readBytesReference());
    }

    public static void writeTo(XContentSource source, StreamOutput out) throws IOException {
        out.writeBytesReference(source.bytes);
    }

    private XContentType contentType() {
        if (contentType == null) {
            contentType = XContentFactory.xContentType(bytes);
        }
        return contentType;
    }

}
