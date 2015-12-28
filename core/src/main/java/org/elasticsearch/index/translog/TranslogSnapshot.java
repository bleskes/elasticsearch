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

import org.elasticsearch.common.io.Channels;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;

public class TranslogSnapshot extends TranslogReader implements Translog.Snapshot {
    private final int totalOperations;
    protected final long length;
    private final long minSeqNo;
    private final long maxSeqNo;
    private final long localCheckpoint;

    private final ByteBuffer reusableBuffer;
    long position;
    int readOperations;
    private BufferedChecksumStreamInput reuse;


    /**
     * Create a snapshot of translog file channel. The length parameter should be consistent with totalOperations and point
     * at the end of the last operation in this snapshot.
     */
    public TranslogSnapshot(long generation, ChannelReference channelReference, long firstOperationOffset, long length, int totalOperations,
                            long minSeqNo, long maxSeqNo, long localCheckpoint) {
        super(generation, channelReference, firstOperationOffset);
        this.length = length;
        this.totalOperations = totalOperations;
        this.minSeqNo = minSeqNo;
        this.maxSeqNo = maxSeqNo;
        this.localCheckpoint = localCheckpoint;
        this.reusableBuffer = ByteBuffer.allocate(1024);
        readOperations = 0;
        position = firstOperationOffset;
        reuse = null;
    }

    @Override
    public final int estimatedTotalOperations() {
        return totalOperations;
    }

    @Override
    public Translog.Operation next() throws IOException {
        if (readOperations < totalOperations) {
            assert readOperations < totalOperations : "readOperations must be less than totalOperations";
            return readOperation();
        } else {
            return null;
        }
    }

    protected final Translog.Operation readOperation() throws IOException {
        final int opSize = readSize(reusableBuffer, position);
        reuse = checksummedStream(reusableBuffer, position, opSize, reuse);
        Translog.Operation op = read(reuse);
        op.location(new Translog.Location(getGeneration(), position, opSize));
        position += opSize;
        readOperations++;
        return op;
    }


    public long sizeInBytes() {
        return length;
    }

    public int totalOperations() {
        return totalOperations;
    }

    @Override
    public long getMinSeqNo() {
        return minSeqNo;
    }

    @Override
    public long getMaxSeqNo() {
        return maxSeqNo;
    }

    @Override
    public long getLocalCheckpoint() {
        return maxSeqNo;
    }

    /**
     * reads an operation at the given position into the given buffer.
     */
    protected void readBytes(ByteBuffer buffer, long position) throws IOException {
        if (position >= length) {
            throw new EOFException("read requested past EOF. pos [" + position + "] end: [" + length + "]");
        }
        if (position < getFirstOperationOffset()) {
            throw new IOException("read requested before position of first ops. pos [" + position + "] first op on: [" + getFirstOperationOffset() + "]");
        }
        Channels.readFromFileChannelWithEofException(channel, position, buffer);
    }

    @Override
    public String toString() {
        return "TranslogSnapshot{" +
                "readOperations=" + readOperations +
                ", position=" + position +
                ", totalOperations=" + totalOperations +
                ", length=" + length +
                ", minSeqNo=" + minSeqNo +
                ", maxSeqNo=" + maxSeqNo +
                ", localCheckpoint=" + localCheckpoint +
                ", reusableBuffer=" + reusableBuffer +
                '}';
    }
}
