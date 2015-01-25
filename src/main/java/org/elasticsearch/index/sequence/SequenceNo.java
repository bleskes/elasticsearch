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

import org.apache.lucene.store.ByteArrayDataInput;
import org.apache.lucene.store.ByteArrayDataOutput;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.RamUsageEstimator;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.util.ByteUtils;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

public class SequenceNo implements Comparable<SequenceNo>, Accountable, ToXContent {

    public static final SequenceNo UNKNOWN = new SequenceNo();

    private final long term;
    private final long counter;

    private SequenceNo() {
        term = counter = -1;
    }

    public SequenceNo(BytesRef bytesRef) {
        ByteArrayDataInput in = new ByteArrayDataInput(bytesRef.bytes, bytesRef.offset, bytesRef.length);
        term = ByteUtils.readVLong(in) - 1;
        counter = ByteUtils.readVLong(in) - 1;
        assert (term >= 0 && counter >= 0) || (term == -1 && counter == -1) : "illegal values: term [" + term + "], counter [" + counter + "]";
    }

    public SequenceNo(long term, long counter) {
        assert (term >= 0 && counter >= 0) || (term == -1 && counter == -1) : "illegal values: term [" + term + "], counter [" + counter + "]";
        this.term = term;
        this.counter = counter;
    }

    @Override
    public int compareTo(SequenceNo o) {
        if (term < o.term) {
            return -1;
        }
        if (term > o.term) {
            return 1;
        }
        return Long.compare(counter, o.counter);
    }

    public long term() {
        return term;
    }

    public long counter() {
        return counter;
    }

    public boolean largerThan(SequenceNo o) {
        return compareTo(o) > 0;
    }

    public static SequenceNo readFrom(StreamInput in) throws IOException {
        return new SequenceNo(in.readVLong() - 1, in.readVLong() - 1);
    }

    public void writeTo(StreamOutput out) throws IOException {
        out.writeVLong(term + 1);
        out.writeVLong(counter + 1);
    }

    @Override
    public long ramBytesUsed() {
        return RamUsageEstimator.NUM_BYTES_LONG * 2;
    }

    @Override
    public Collection<Accountable> getChildResources() {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return "_seq#[" + term + "][" + counter + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SequenceNo) {
            SequenceNo other = (SequenceNo) obj;
            return other.term == term && other.counter == counter;
        }
        return false;
    }

    public BytesRef toBytes() {
        byte[] bytes = new byte[18]; // maximum needed may not be used.
        ByteArrayDataOutput output = new ByteArrayDataOutput(bytes);
        ByteUtils.writeVLong(output, term + 1);
        ByteUtils.writeVLong(output, counter + 1);
        return new BytesRef(bytes, 0, output.getPosition());
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, ToXContent.Params params) throws IOException {
        return builder.startObject().field(Fields.TERM, term()).field(Fields.COUNTER, counter()).endObject();
    }

    static final class Fields {
        static final XContentBuilderString TERM = new XContentBuilderString("term");
        static final XContentBuilderString COUNTER = new XContentBuilderString("counter");
    }
}
