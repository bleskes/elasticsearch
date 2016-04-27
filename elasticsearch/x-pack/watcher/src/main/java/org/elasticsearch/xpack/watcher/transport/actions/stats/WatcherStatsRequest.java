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

package org.elasticsearch.xpack.watcher.transport.actions.stats;

import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.support.master.MasterNodeReadRequest;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

/**
 * The Request to get the watcher stats
 */
public class WatcherStatsRequest extends MasterNodeReadRequest<WatcherStatsRequest> {

    private boolean includeCurrentWatches;
    private boolean includeQueuedWatches;

    public WatcherStatsRequest() {
    }

    public boolean includeCurrentWatches() {
        return includeCurrentWatches;
    }

    public void includeCurrentWatches(boolean currentWatches) {
        this.includeCurrentWatches = currentWatches;
    }

    public boolean includeQueuedWatches() {
        return includeQueuedWatches;
    }

    public void includeQueuedWatches(boolean includeQueuedWatches) {
        this.includeQueuedWatches = includeQueuedWatches;
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        includeCurrentWatches = in.readBoolean();
        includeQueuedWatches = in.readBoolean();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeBoolean(includeCurrentWatches);
        out.writeBoolean(includeQueuedWatches);
    }

    @Override
    public String toString() {
        return "watcher_stats";
    }
}
