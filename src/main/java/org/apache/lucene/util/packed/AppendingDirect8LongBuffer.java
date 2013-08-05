package org.apache.lucene.util.packed;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import org.elasticsearch.index.fielddata.ordinals.Ordinals;

import java.util.Arrays;

/**
 * Utility class to buffer a list of signed longs in memory. This class only
 * supports appending and is optimized for the case where values are close to
 * each other.
 *
 * @lucene.internal
 */
public final class AppendingDirect8LongBuffer extends XAbstractAppendingLongBuffer {

    /**
     * @param initialPageCount the initial number of pages
     * @param pageSize         the size of a single page
     */
    public AppendingDirect8LongBuffer(int initialPageCount, int pageSize, float acceptableOverheadRatio) {
        super(initialPageCount, pageSize, acceptableOverheadRatio);

    }

    public AppendingDirect8LongBuffer(float acceptableOverheadRatio) {
        super(16, 1024, acceptableOverheadRatio);
    }


    public AppendingDirect8LongBuffer() {
        this(16, 1024, PackedInts.DEFAULT);
    }

    @Override
    long get(int block, int element) {
        if (block == valuesOff) {
            return pending[element];
        } else {
            return values[block].get(element);
        }
    }

    @Override
    int get(int block, int element, long[] arr, int off, int len) {
        long[] v;
        int sysCopyToRead;
        if (block == valuesOff) {
            sysCopyToRead = Math.min(len, pendingOff - element);
            v = pending;
        } else {
           /* packed block */
            return values[block].get(element, arr, off, len);
        }
        System.arraycopy(v, element, arr, off, sysCopyToRead);
        return sysCopyToRead;
    }

    @Override
    void packPendingValues() {
        long minValue = pending[0];
        long maxValue = pending[0];
        for (int i = 1; i < pendingOff; ++i) {
            minValue = Math.min(minValue, pending[i]);
            maxValue = Math.max(maxValue, pending[i]);
        }

        if (minValue == maxValue && minValue == 0L) {
            values[valuesOff] = new PackedInts.NullReader(pendingOff);
            return;
        }

        // build a new packed reader
        final int bitsRequired = minValue < 0 ? 64 : PackedInts.bitsRequired(maxValue);
        final PackedInts.Mutable mutable = new Direct8(pendingOff);
        for (int i = 0; i < pendingOff; ) {
            i += mutable.set(i, pending, i, pendingOff - i);
        }
        values[valuesOff] = mutable;
    }


    static public class Iter implements Ordinals.Docs.Iter {

        final AppendingDirect8LongBuffer ordinals;
        int startBlock, endBlock;
        int currentOffset, currentEndOffset, endOffsetInEndBlock;
        Direct8 currentBlock;

        public Iter(AppendingDirect8LongBuffer ordinals) {
            this.ordinals = ordinals;
        }

        public void reset(long startOffset, long endOffset) {
            startBlock = (int) (startOffset >> ordinals.pageShift);
            endBlock = (int) (endOffset >> ordinals.pageShift);
            currentOffset = (int) (startOffset & ordinals.pageMask);
            endOffsetInEndBlock = (int) (endOffset & ordinals.pageMask);
            currentBlock = (Direct8) ordinals.values[startBlock];
            if (startBlock == endBlock) {
                currentEndOffset = endOffsetInEndBlock;
            } else {
                currentEndOffset = currentBlock.size();
            }
        }


        @Override
        public long next() {
            if (currentOffset >= currentEndOffset) {
                if (startBlock == endBlock) {
                    return 0L; // done
                }
                startBlock++;
                currentBlock = (Direct8) ordinals.values[startBlock];
                if (startBlock == endBlock) {
                    currentEndOffset = endOffsetInEndBlock;
                } else {
                    currentEndOffset = currentBlock.size();
                }
                currentOffset = 0;
            }
            return 1L + currentBlock.get(currentOffset++);
        }

    }
}
