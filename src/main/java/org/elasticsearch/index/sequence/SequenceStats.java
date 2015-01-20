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

package org.elasticsearch.index.sequence;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Streamable;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;

import java.io.IOException;

/**
 */
public class SequenceStats implements Streamable, ToXContent {

    // TODO: not null on shard level, null in aggregates - review. Doesn't feel good.

    @Nullable
    private SequenceNo consensusNo;
    @Nullable
    private SequenceNo maxConsecutiveSeqNo;
    @Nullable
    private SequenceNo maxSeqNo;

    private long consensusDiff;

    public SequenceStats() {
        consensusNo = null;
        maxConsecutiveSeqNo = null;
        maxSeqNo = null;
        consensusDiff = -1;
    }

    public SequenceStats(SequenceNo consensusNo, SequenceNo maxConsecutiveSeqNo, SequenceNo maxSeqNo) {
        this.consensusNo = consensusNo;
        this.maxSeqNo = maxSeqNo;
        this.maxConsecutiveSeqNo = maxConsecutiveSeqNo;
        this.consensusDiff = maxSeqNo.counter() - consensusNo.counter();
    }

    public void add(SequenceStats stats) {
        if (stats == null) {
            return;
        }
        assert maxConsecutiveSeqNo == null;
        assert maxSeqNo == null;
        assert consensusNo == null;
        consensusDiff = Math.max(consensusDiff, stats.consensusDiff);
    }


    public long consensusDiff() {
        return consensusDiff;
    }

    @Nullable
    public SequenceNo maxSequenceNo() {
        return maxSeqNo;
    }

    @Nullable
    public SequenceNo maxConsecutiveSeqNo() {
        return maxConsecutiveSeqNo;
    }

    @Nullable
    public SequenceNo consensusNo() {
        return consensusNo;
    }


    public static SequenceStats readStoreStats(StreamInput in) throws IOException {
        SequenceStats store = new SequenceStats();
        store.readFrom(in);
        return store;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        consensusDiff = in.readLong();
        if (in.readBoolean()) {
            maxSeqNo = SequenceNo.readFrom(in);
        } else {
            maxSeqNo = null;
        }
        if (in.readBoolean()) {
            maxConsecutiveSeqNo = SequenceNo.readFrom(in);
        } else {
            maxConsecutiveSeqNo = null;
        }
        if (in.readBoolean()) {
            consensusNo = SequenceNo.readFrom(in);
        } else {
            consensusNo = null;
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeLong(consensusDiff);
        if (maxSeqNo == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            maxSeqNo.writeTo(out);
        }
        if (maxConsecutiveSeqNo == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            maxConsecutiveSeqNo.writeTo(out);
        }
        if (consensusNo == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            consensusNo.writeTo(out);
        }
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(Fields.SEQ_NO);
        builder.field(Fields.CONSENSUS_DIFF, consensusDiff);
        if (consensusNo != null) {
            builder.field(Fields.CONSENSUS_NO, consensusNo, params);
        }
        if (maxSeqNo != null) {
            builder.field(Fields.MAX_SEQ_NO, maxSeqNo, params);
        }
        if (maxConsecutiveSeqNo != null) {
            builder.field(Fields.MAX_CONSECUTIVE_SEQ_NO, maxConsecutiveSeqNo, params);
        }
        builder.endObject();
        return builder;
    }

    static final class Fields {
        static final XContentBuilderString SEQ_NO = new XContentBuilderString("sequence_no");
        static final XContentBuilderString CONSENSUS_DIFF = new XContentBuilderString("consensus_diff");
        static final XContentBuilderString CONSENSUS_NO = new XContentBuilderString("consensus");
        static final XContentBuilderString MAX_CONSECUTIVE_SEQ_NO = new XContentBuilderString("max_consecutive");
        static final XContentBuilderString MAX_SEQ_NO = new XContentBuilderString("max");
    }
}
