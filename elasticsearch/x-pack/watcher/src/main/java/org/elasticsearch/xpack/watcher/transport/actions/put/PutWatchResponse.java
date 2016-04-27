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

package org.elasticsearch.xpack.watcher.transport.actions.put;

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

/**
 * The Response for a put watch action
 */
public class PutWatchResponse extends ActionResponse {

    private String id;
    private long version;
    private boolean created;

    PutWatchResponse() {
    }

    public PutWatchResponse(String id, long version, boolean created) {
        this.id = id;
        this.version = version;
        this.created = created;
    }

    public String getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }

    public boolean isCreated() {
        return created;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(id);
        out.writeVLong(version);
        out.writeBoolean(created);
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        id = in.readString();
        version = in.readVLong();
        created = in.readBoolean();
    }
}
