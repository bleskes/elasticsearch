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
package org.elasticsearch.index.translog;

import org.elasticsearch.action.support.ToXContentToBytes;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Streamable;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
import org.elasticsearch.index.seqno.SequenceNumbersService;

import java.io.IOException;

/**
 *
 */
public class TranslogStats extends ToXContentToBytes implements Streamable {

    private long translogSizeInBytes;
    private int numberOfOperations;
    private long maxSeqNo = SequenceNumbersService.UNASSIGNED_SEQ_NO;
    private long localCheckpoint = SequenceNumbersService.UNASSIGNED_SEQ_NO;


    public TranslogStats() {
    }

    public TranslogStats(int numberOfOperations, long translogSizeInBytes, long maxSeqNo, long localCheckpoint) {
        if (numberOfOperations < 0) {
            throw new IllegalArgumentException("numberOfOperations must be >= 0");
        }
        if (translogSizeInBytes < 0) {
            throw new IllegalArgumentException("translogSizeInBytes must be >= 0");
        }
        if (maxSeqNo < SequenceNumbersService.NO_OPS_PERFORMED) {
            throw new IllegalArgumentException("numberOfOperations must be >= " + SequenceNumbersService.NO_OPS_PERFORMED);
        }
        if (localCheckpoint < SequenceNumbersService.NO_OPS_PERFORMED) {
            throw new IllegalArgumentException("translogSizeInBytes must be >= " + SequenceNumbersService.NO_OPS_PERFORMED);
        }
        assert translogSizeInBytes >= 0 : "translogSizeInBytes must be >= 0, got [" + translogSizeInBytes + "]";
        this.numberOfOperations = numberOfOperations;
        this.translogSizeInBytes = translogSizeInBytes;
        this.maxSeqNo = maxSeqNo;
        this.localCheckpoint = localCheckpoint;
    }

    public void add(TranslogStats translogStats) {
        if (translogStats == null) {
            return;
        }

        this.numberOfOperations += translogStats.numberOfOperations;
        this.translogSizeInBytes += translogStats.translogSizeInBytes;
    }

    public long getTranslogSizeInBytes() {
        return translogSizeInBytes;
    }

    public long numberOfOperations() {
        return numberOfOperations;
    }

    /** the maximum sequence number seen so far */
    public long getMaxSeqNo() {
        return maxSeqNo;
    }

    /** the maximum sequence number for which all previous operations (including) have been completed */
    public long getLocalCheckpoint() {
        return localCheckpoint;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(Fields.TRANSLOG);
        builder.field(Fields.OPERATIONS, numberOfOperations);
        builder.byteSizeField(Fields.SIZE_IN_BYTES, Fields.SIZE, translogSizeInBytes);
        if (maxSeqNo != SequenceNumbersService.UNASSIGNED_SEQ_NO) {
            builder.field(Fields.MAX_SEQ_NO, maxSeqNo);
        }
        if (localCheckpoint != SequenceNumbersService.UNASSIGNED_SEQ_NO) {
            builder.field(Fields.LOCAL_CHECKPOINT, localCheckpoint);
        }
        builder.endObject();
        return builder;
    }

    static final class Fields {
        static final XContentBuilderString TRANSLOG = new XContentBuilderString("translog");
        static final XContentBuilderString OPERATIONS = new XContentBuilderString("operations");
        static final XContentBuilderString SIZE = new XContentBuilderString("size");
        static final XContentBuilderString SIZE_IN_BYTES = new XContentBuilderString("size_in_bytes");
        static final XContentBuilderString MAX_SEQ_NO = new XContentBuilderString("max_seq_no");
        static final XContentBuilderString LOCAL_CHECKPOINT = new XContentBuilderString("local_checkpoint");
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        numberOfOperations = in.readVInt();
        translogSizeInBytes = in.readVLong();
        maxSeqNo = in.readZLong();
        localCheckpoint = in.readZLong();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeVInt(numberOfOperations);
        out.writeVLong(translogSizeInBytes);
        out.writeZLong(maxSeqNo);
        out.writeZLong(localCheckpoint);
    }
}
