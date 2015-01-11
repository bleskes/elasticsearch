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
package org.elasticsearch.action;

import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
import org.elasticsearch.index.sequence.SequenceNo;

import java.io.IOException;

public abstract class ActionDocWriteResponse extends ActionWriteResponse implements ToXContent {

    private String index;
    private String id;
    private String type;
    private long version;
    private SequenceNo sequenceNo;

    protected ActionDocWriteResponse() {
    }

    public ActionDocWriteResponse(String index, String type, String id, long version, SequenceNo sequenceNo) {
        this.index = index;
        this.id = id;
        this.type = type;
        this.version = version;
        this.sequenceNo = sequenceNo;
    }


    /**
     * The index the document was deleted from.
     */
    public String getIndex() {
        return this.index;
    }

    /**
     * The type of the document deleted.
     */
    public String getType() {
        return this.type;
    }

    /**
     * The id of the document deleted.
     */
    public String getId() {
        return this.id;
    }

    /**
     * The version of the delete operation.
     */
    public long getVersion() {
        return this.version;
    }


    public SequenceNo getSequenceNo() {
        return this.sequenceNo;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        index = in.readSharedString();
        type = in.readSharedString();
        id = in.readString();
        version = in.readLong();
        sequenceNo = SequenceNo.readFrom(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeSharedString(index);
        out.writeSharedString(type);
        out.writeString(id);
        out.writeLong(version);
        sequenceNo.writeTo(out);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.field(Fields._INDEX, index)
                .field(Fields._TYPE, type)
                .field(Fields._ID, id)
                .field(Fields._VERSION, version)
                .startObject(Fields._SEQNO)
                .field(Fields._SEQNO_TERM, sequenceNo.term())
                .field(Fields._SEQNO_COUNTER, sequenceNo.counter())
                .endObject()
                .value(getShardInfo());
        return builder;
    }

    static final class Fields {
        static final XContentBuilderString _INDEX = new XContentBuilderString("_index");
        static final XContentBuilderString _TYPE = new XContentBuilderString("_type");
        static final XContentBuilderString _ID = new XContentBuilderString("_id");
        static final XContentBuilderString _VERSION = new XContentBuilderString("_version");
        static final XContentBuilderString _SEQNO = new XContentBuilderString("_seqno");
        static final XContentBuilderString _SEQNO_TERM = new XContentBuilderString("term");
        static final XContentBuilderString _SEQNO_COUNTER = new XContentBuilderString("counter");
    }
}
