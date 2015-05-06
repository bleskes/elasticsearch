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

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.watcher.support.xcontent.XContentSource;

import java.io.IOException;

public class GetWatchResponse extends ActionResponse {

    private String id;
    private long version = -1;
    private boolean found = false;
    private XContentSource source;

    GetWatchResponse() {
    }

    public GetWatchResponse(String id, long version, boolean found, BytesReference source) {
        assert !found && source == null || found && source.length() > 0;
        this.id = id;
        this.version = version;
        this.found = found;
        this.source = found ? new XContentSource(source) : null;
    }

    public String getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }


    public boolean isFound() {
        return found;
    }

    public XContentSource getSource() {
        return source;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        id = in.readString();
        found = in.readBoolean();
        version = in.readLong();
        source = found ? XContentSource.readFrom(in) : null;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(id);
        out.writeBoolean(found);
        out.writeLong(version);
        if (found) {
            XContentSource.writeTo(source, out);
        }
    }
}
