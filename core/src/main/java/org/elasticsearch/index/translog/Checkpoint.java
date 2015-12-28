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

import org.apache.lucene.util.RamUsageEstimator;
import org.elasticsearch.Version;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.io.stream.InputStreamStreamInput;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.index.seqno.SequenceNumbersService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;

/**
 */
class Checkpoint {

    // nocommit - double check implications of size change
    static final int EXPECTED_BUFFER_SIZE =
            +RamUsageEstimator.NUM_BYTES_INT  // version
                    + RamUsageEstimator.NUM_BYTES_INT  // ops
                    + RamUsageEstimator.NUM_BYTES_LONG // offset
                    + RamUsageEstimator.NUM_BYTES_LONG // generation
                    + RamUsageEstimator.NUM_BYTES_LONG // minSeqNo
                    + RamUsageEstimator.NUM_BYTES_LONG // maxSeqNo
                    + RamUsageEstimator.NUM_BYTES_LONG;// localCheckpoint
    final long offset;
    final int numOps;
    final long generation;
    final long minSeqNo;
    final long maxSeqNo;
    final long localCheckpoint;

    Checkpoint(long offset, int numOps, long generation, long minSeqNo, long maxSeqNp, long localCheckpoint) {
        this.offset = offset;
        this.numOps = numOps;
        this.generation = generation;
        this.minSeqNo = minSeqNo;
        this.maxSeqNo = maxSeqNp;
        this.localCheckpoint = localCheckpoint;
    }

    Checkpoint(StreamInput in) throws IOException {
        offset = in.readLong();
        numOps = in.readInt();
        generation = in.readLong();
        if (in.getVersion().onOrAfter(Version.V_3_0_0)) {
            minSeqNo = in.readLong();
            maxSeqNo = in.readLong();
            localCheckpoint = in.readLong();
        } else {
            minSeqNo = SequenceNumbersService.UNASSIGNED_SEQ_NO;
            maxSeqNo = SequenceNumbersService.UNASSIGNED_SEQ_NO;
            localCheckpoint = SequenceNumbersService.UNASSIGNED_SEQ_NO;
        }
    }

    private void write(FileChannel channel) throws IOException {
        final BytesStreamOutput out = new BytesStreamOutput(EXPECTED_BUFFER_SIZE);
        out.setVersion(Version.CURRENT);
        Version.writeVersion(Version.CURRENT, out);
        write(out);
        out.bytes().writeTo(channel);
    }

    private void write(StreamOutput out) throws IOException {
        out.writeLong(offset);
        out.writeInt(numOps);
        out.writeLong(generation);
        out.writeLong(minSeqNo);
        out.writeLong(maxSeqNo);
        out.writeLong(localCheckpoint);
    }

    @Override
    public String toString() {
        return "Checkpoint{" +
                "offset=" + offset +
                ", numOps=" + numOps +
                ", generation=" + generation +
                ", minSeqNo=" + minSeqNo +
                ", maxSeqNo=" + maxSeqNo +
                ", localCheckpoint=" + localCheckpoint +
                '}';
    }

    public static Checkpoint read(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            StreamInput in = new InputStreamStreamInput(inputStream);
            final Version version;
            if (Files.size(path) == 20) {
                version = Version.V_2_0_0;
            } else {
                version = Version.readVersion(in);
            }
            in.setVersion(version);
            return new Checkpoint(in);
        }
    }

    public static void write(Path checkpointFile, Checkpoint checkpoint, OpenOption... options) throws IOException {
        try (FileChannel channel = FileChannel.open(checkpointFile, options)) {
            checkpoint.write(channel);
            channel.force(false);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Checkpoint that = (Checkpoint) o;

        if (offset != that.offset) {
            return false;
        }
        if (numOps != that.numOps) {
            return false;
        }
        if (generation != that.generation) {
            return false;
        }
        if (minSeqNo != that.minSeqNo) {
            return false;
        }
        if (maxSeqNo != that.maxSeqNo) {
            return false;
        }
        return localCheckpoint == that.localCheckpoint;

    }

    @Override
    public int hashCode() {
        int result = (int) (offset ^ (offset >>> 32));
        result = 31 * result + numOps;
        result = 31 * result + Long.hashCode(generation);
        result = 31 * result + Long.hashCode(minSeqNo);
        result = 31 * result + Long.hashCode(maxSeqNo);
        result = 31 * result + Long.hashCode(localCheckpoint);
        return result;
    }

    public boolean pendingWrites() {
        return maxSeqNo != SequenceNumbersService.UNASSIGNED_SEQ_NO &&
                maxSeqNo != SequenceNumbersService.NO_OPS_PERFORMED &&
                maxSeqNo != localCheckpoint;
    }
}
