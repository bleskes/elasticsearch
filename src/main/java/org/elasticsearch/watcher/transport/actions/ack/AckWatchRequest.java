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

package org.elasticsearch.watcher.transport.actions.ack;

import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.ValidateActions;
import org.elasticsearch.action.support.master.MasterNodeOperationRequest;
import org.elasticsearch.watcher.watch.WatchStore;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

/**
 * A delete watch request to delete an watch by name (id)
 */
public class AckWatchRequest extends MasterNodeOperationRequest<AckWatchRequest> {

    private String watchName;

    public AckWatchRequest() {
    }

    public AckWatchRequest(String watchName) {
        this.watchName = watchName;
    }

    /**
     * @return The name of the watch to be acked
     */
    public String getWatchName() {
        return watchName;
    }

    /**
     * Sets the name of the watch to be acked
     */
    public void setWatchName(String watchName) {
        this.watchName = watchName;
    }

    @Override
    public ActionRequestValidationException validate() {
        ActionRequestValidationException validationException = null;
        if (watchName == null){
            validationException = ValidateActions.addValidationError("watch name is missing", validationException);
        }
        return validationException;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        watchName = in.readString();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(watchName);
    }

    @Override
    public String toString() {
        return "ack {[" + WatchStore.INDEX + "][" + watchName + "]}";
    }
}
