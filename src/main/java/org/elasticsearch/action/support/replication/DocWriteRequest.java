/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.action.support.replication;

import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.DocumentRequest;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.lucene.uid.Versions;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.index.sequence.SequenceNo;

import java.io.IOException;

import static org.elasticsearch.action.ValidateActions.addValidationError;

public abstract class DocWriteRequest<T extends DocWriteRequest> extends ShardReplicationOperationRequest<T> implements DocumentRequest<T> {

    private String type;
    private String id;
    @Nullable
    private String routing;
    private boolean refresh;
    private long version = Versions.MATCH_ANY;
    private VersionType versionType = VersionType.INTERNAL;

    // TODO: this is only relevant for replicas - implement ReplicaIndexRequest, inheriting from this class
    private SequenceNo sequenceNo = SequenceNo.UNKNOWN;

    public DocWriteRequest() {
    }

    public DocWriteRequest(String index) {
        this.index = index;
    }

    public DocWriteRequest(String index, String type) {
        this.index = index;
        this.type = type;
    }

    public DocWriteRequest(String index, String type, String id) {
        this.index = index;
        this.type = type;
        this.id = id;
    }

    /**
     * Copy constructor that creates a new delete request that is a copy of the one provided as an argument.
     */
    public DocWriteRequest(T request) {
        this(request, request);
    }

    /**
     * Copy constructor that creates a new delete request that is a copy of the one provided as an argument.
     * The new request will inherit though headers and context from the original request that caused it.
     */
    public DocWriteRequest(T request, ActionRequest originalRequest) {
        super(request, originalRequest);
        this.type = request.type();
        this.id = request.id();
        this.routing = request.routing();
        this.refresh = request.refresh();
        this.version = request.version();
        this.versionType = request.versionType();
    }

    /**
     * Creates a delete request caused by some other request, which is provided as an
     * argument so that its headers and context can be copied to the new request
     */
    public DocWriteRequest(ActionRequest request) {
        super(request);
    }

    @Override
    public ActionRequestValidationException validate() {
        ActionRequestValidationException validationException = super.validate();
        if (type == null) {
            validationException = addValidationError("type is missing", validationException);
        }
        if (id == null) {
            validationException = addValidationError("id is missing", validationException);
        }
        if (!versionType.validateVersionForWrites(version)) {
            validationException = addValidationError("illegal version value [" + version + "] for version type [" + versionType.name() + "]", validationException);
        }
        return validationException;
    }

    /**
     * The type of the document to change.
     */
    public String type() {
        return type;
    }

    /**
     * Sets the type of the document to change.
     */
    public T type(String type) {
        this.type = type;
        return (T) this;
    }

    /**
     * The id of the document to change.
     */
    public String id() {
        return id;
    }

    /**
     * Sets the id of the document to change.
     */
    public T id(String id) {
        this.id = id;
        return (T) this;
    }

    /**
     * Sets the parent id of this document.
     */
    public T parent(String parent) {
        if (routing == null) {
            routing = parent;
        }
        return (T) this;
    }

    /**
     * Controls the shard routing of the request. Using this value to hash the shard
     * and not the id.
     */
    public T routing(String routing) {
        if (routing != null && routing.length() == 0) {
            this.routing = null;
        } else {
            this.routing = routing;
        }
        return (T) this;
    }

    /**
     * Controls the shard routing of the request. Using this value to hash the shard
     * and not the id.
     */
    public String routing() {
        return this.routing;
    }

    /**
     * Should a refresh be executed post this index operation causing the operation to
     * be searchable. Note, heavy indexing should not set this to <tt>true</tt>. Defaults
     * to <tt>false</tt>.
     */
    public T refresh(boolean refresh) {
        this.refresh = refresh;
        return (T) this;
    }

    public boolean refresh() {
        return this.refresh;
    }

    /**
     * Sets the version, which will cause the operation to only be performed if a matching
     * version exists and no changes happened on the doc since then.
     */
    public T version(long version) {
        this.version = version;
        return (T) this;
    }

    public long version() {
        return this.version;
    }

    public T versionType(VersionType versionType) {
        this.versionType = versionType;
        return (T) this;
    }

    public SequenceNo sequenceNo() {
        return this.sequenceNo;
    }

    public T sequenceNo(SequenceNo sequenceNo) {
        this.sequenceNo = sequenceNo;
        return (T) this;
    }

    public VersionType versionType() {
        return this.versionType;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        type = in.readSharedString();
        id = in.readString();
        routing = in.readOptionalString();
        refresh = in.readBoolean();
        version = in.readLong();
        versionType = VersionType.fromValue(in.readByte());
        sequenceNo = SequenceNo.readFrom(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeSharedString(type);
        out.writeString(id);
        out.writeOptionalString(routing());
        out.writeBoolean(refresh);
        out.writeLong(version);
        out.writeByte(versionType.getValue());
        sequenceNo.writeTo(out);
    }

    @Override
    public String toString() {
        return getRequestName() + " {[" + index + "][" + type + "][" + id + "]}";
    }

    /** a short lower cased name to describe this request */
    protected abstract String getRequestName();

}
