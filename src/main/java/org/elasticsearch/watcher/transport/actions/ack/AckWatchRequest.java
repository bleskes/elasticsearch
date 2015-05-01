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
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.unit.TimeValue;

import java.io.IOException;

/**
 * A ack watch request to ack a watch by name (id)
 */
public class AckWatchRequest extends MasterNodeOperationRequest<AckWatchRequest> {

    private static final TimeValue DEFAULT_TIMEOUT = TimeValue.timeValueSeconds(10);

    private String id;

    public AckWatchRequest() {
        this(null);
    }

    public AckWatchRequest(String id) {
        this.id = id;
        masterNodeTimeout(DEFAULT_TIMEOUT);
    }

    /**
     * @return The name of the watch to be acked
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the name of the watch to be acked
     */
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public ActionRequestValidationException validate() {
        ActionRequestValidationException validationException = null;
        if (id == null){
            validationException = ValidateActions.addValidationError("watch id is missing", validationException);
        }
        return validationException;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        id = in.readString();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(id);
    }

    @Override
    public String toString() {
        return "ack [" + id + "]";
    }
}
