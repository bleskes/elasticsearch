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

package org.elasticsearch.watcher.transport.actions.delete;

import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.ValidateActions;
import org.elasticsearch.action.support.master.MasterNodeRequest;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.lucene.uid.Versions;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.watcher.support.validation.Validation;

import java.io.IOException;

/**
 * A delete watch request to delete an watch by name (id)
 */
public class DeleteWatchRequest extends MasterNodeRequest<DeleteWatchRequest> {

    private static final TimeValue DEFAULT_TIMEOUT = TimeValue.timeValueSeconds(10);

    private String id;
    private long version = Versions.MATCH_ANY;
    private boolean force = false;

    public DeleteWatchRequest() {
        this(null);
    }

    public DeleteWatchRequest(String id) {
        this.id = id;
        masterNodeTimeout(DEFAULT_TIMEOUT);
    }

    /**
     * @return The name of the watch to be deleted
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the name of the watch to be deleted
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return true if this request should ignore locking false if not
     */
    public boolean isForce() {
        return force;
    }

    /**
     * @param force Sets weither this request should ignore locking and force the delete even if lock is unavailable.
     */
    public void setForce(boolean force) {
        this.force = force;
    }


    /**
     * Sets the version, which will cause the delete operation to only be performed if a matching
     * version exists and no changes happened on the doc since then.
     */
    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    @Override
    public ActionRequestValidationException validate() {
        ActionRequestValidationException validationException = null;
        if (id == null){
            validationException = ValidateActions.addValidationError("watch id is missing", validationException);
        }
        Validation.Error error = Validation.watchId(id);
        if (error != null) {
            validationException = ValidateActions.addValidationError(error.message(), validationException);
        }
        return validationException;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        id = in.readString();
        version = in.readLong();
        force = in.readBoolean();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(id);
        out.writeLong(version);
        out.writeBoolean(force);
    }

    @Override
    public String toString() {
        return "delete [" + id + "]";
    }
}
