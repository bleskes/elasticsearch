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

package org.elasticsearch.xpack.watcher.transport.actions.ack;

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.xpack.watcher.watch.WatchStatus;

import java.io.IOException;

/**
 * This class contains the status of the watch. If the watch was successfully acked
 * this will be reflected in the watch status.
 */
public class AckWatchResponse extends ActionResponse {

    private WatchStatus status;

    public AckWatchResponse() {
    }

    public AckWatchResponse(@Nullable WatchStatus status) {
        this.status = status;
    }

    /**
     * @return The watch status
     */
    public WatchStatus getStatus() {
        return status;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        status = in.readBoolean() ? WatchStatus.read(in) : null;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeBoolean(status != null);
        if (status != null) {
            status.writeTo(out);
        }
    }
}
