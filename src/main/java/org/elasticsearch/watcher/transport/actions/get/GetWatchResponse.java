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

package org.elasticsearch.watcher.transport.actions.get;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.search.lookup.SourceLookup;

import java.io.IOException;
import java.util.Map;

public class GetWatchResponse extends ActionResponse {

    private boolean exists = false;
    private String id;
    private long version;
    private BytesReference source;

    private Map<String, Object> sourceAsMap;

    public GetWatchResponse(boolean found, String id, long version, BytesReference source) {
        this.exists = found;
        this.id = id;
        this.version = version;
        this.source = source;
        GetResponse foo;
    }

    public GetWatchResponse() {
    }

    public BytesReference source() {
        return source;
    }

    public Map<String, Object> sourceAsMap() throws ElasticsearchParseException {
        if (source == null) {
            return null;
        }
        if (sourceAsMap != null) {
            return sourceAsMap;
        }

        sourceAsMap = SourceLookup.sourceAsMap(source);
        return sourceAsMap;
    }


    public boolean exists() {
        return exists;
    }

    public String id() {
        return id;
    }

    public long version() {
        return version;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        exists = in.readBoolean();
        if (exists) {
            id = in.readString();
            version = in.readLong();
            source = in.readBytesReference();
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeBoolean(exists);
        if (exists) {
            out.writeString(id);
            out.writeLong(version);
            out.writeBytesReference(source);
        }
    }
}
