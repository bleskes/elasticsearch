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
public final class AppendingDirectLongBuffer extends XAbstractAppendingLongBuffer {

    long[][] direct_values;

    /**
     * @param initialPageCount the initial number of pages
     * @param pageSize         the size of a single page
     */
    public AppendingDirectLongBuffer(int initialPageCount, int pageSize, float acceptableOverheadRatio) {
        super(initialPageCount, pageSize, acceptableOverheadRatio);
        direct_values = new long[initialPageCount][];

    }

    public AppendingDirectLongBuffer(float acceptableOverheadRatio) {
        super(16, 1024, acceptableOverheadRatio);
    }


    public AppendingDirectLongBuffer() {
        this(16, 1024, PackedInts.DEFAULT);
    }

    @Override
    void grow(int newBlockCount) {
        super.grow(newBlockCount);
        direct_values = Arrays.copyOf(direct_values, newBlockCount);
    }

    @Override
    long get(int block, int element) {
        if (block == valuesOff) {
            return pending[element];
        } else {
            return direct_values[block][element];
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
            v = direct_values[block];
            sysCopyToRead = Math.min(len, v.length - element);
        }
        System.arraycopy(v, element, arr, off, sysCopyToRead);
        return sysCopyToRead;
    }

    @Override
    void packPendingValues() {
        values[valuesOff] = new PackedInts.NullReader(pendingOff);
        direct_values[valuesOff] = pending;
        pending = new long[pending.length];

    }


    static public class Iter implements Ordinals.Docs.Iter {

        final AppendingDirectLongBuffer ordinals;
        int startBlock, endBlock;
        int currentOffset, currentEndOffset, endOffsetInEndBlock;

        public Iter(AppendingDirectLongBuffer ordinals) {
            this.ordinals = ordinals;
        }

        public void reset(long startOffset, long endOffset) {
            startBlock = (int) (startOffset >> ordinals.pageShift);
            endBlock = (int) (endOffset >> ordinals.pageShift);
            currentOffset = (int) (startOffset & ordinals.pageMask);
            endOffsetInEndBlock = (int) (endOffset & ordinals.pageMask);
            if (startBlock == endBlock) {
                currentEndOffset = endOffsetInEndBlock;
            } else {
                currentEndOffset = ordinals.direct_values[startBlock].length;
            }
        }


        @Override
        public long next() {
            if (currentOffset >= currentEndOffset) {
                if (startBlock == endBlock) {
                    return 0L; // done
                }
                startBlock++;
                if (startBlock == endBlock) {
                    currentEndOffset = endOffsetInEndBlock;
                } else {
                    currentEndOffset = ordinals.direct_values[startBlock].length;
                }
                currentOffset = 0;
            }
            return 1L + ordinals.direct_values[startBlock][currentOffset++];
        }

    }
}
