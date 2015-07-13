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

package org.elasticsearch.watcher.transport.actions.put;


import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.ValidateActions;
import org.elasticsearch.action.support.master.MasterNodeReadRequest;
import org.elasticsearch.action.support.master.MasterNodeRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.watcher.client.WatchSourceBuilder;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.watcher.support.validation.Validation;

import java.io.IOException;

/**
 * This request class contains the data needed to create a watch along with the name of the watch.
 * The name of the watch will become the ID of the indexed document.
 */
public class PutWatchRequest extends MasterNodeRequest<PutWatchRequest> {

    private static final TimeValue DEFAULT_TIMEOUT = TimeValue.timeValueSeconds(10);

    private String id;
    private BytesReference source;

    PutWatchRequest() {
    }

    public PutWatchRequest(String id, WatchSourceBuilder source) {
        this(id, source.buildAsBytes(XContentType.JSON));
    }

    public PutWatchRequest(String id, BytesReference source) {
        this.id = id;
        this.source = source;
        masterNodeTimeout(DEFAULT_TIMEOUT);
    }

    /**
     * @return The name that will be the ID of the indexed document
     */
    public String getId() {
        return id;
    }

    /**
     * Set the watch name
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return The source of the watch
     */
    public BytesReference getSource() {
        return source;
    }

    /**
     * Set the source of the watch
     */
    public void setSource(WatchSourceBuilder source) {
        setSource(source.buildAsBytes(XContentType.JSON));
    }

    /**
     * Set the source of the watch
     */
    public void setSource(BytesReference source) {
        this.source = source;
    }

    @Override
    public ActionRequestValidationException validate() {
        ActionRequestValidationException validationException = null;
        if (id == null) {
            validationException = ValidateActions.addValidationError("watch name is missing", validationException);
        }
        Validation.Error error = Validation.watchId(id);
        if (error != null) {
            validationException = ValidateActions.addValidationError(error.message(), validationException);
        }
        if (source == null) {
            validationException = ValidateActions.addValidationError("watch source is missing", validationException);
        }
        return validationException;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        id = in.readString();
        source = in.readBytesReference();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(id);
        out.writeBytesReference(source);
    }

}
