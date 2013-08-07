/*
 * Licensed to ElasticSearch and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. ElasticSearch licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
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

package org.elasticsearch.index.fielddata.ordinals;

import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.LongsRef;
import org.apache.lucene.util.RamUsageEstimator;
import org.apache.lucene.util.packed.AppendingDirect8LongBuffer;
import org.apache.lucene.util.packed.MonotonicAppendingLongBuffer;
import org.apache.lucene.util.packed.PackedInts;
import org.apache.lucene.util.packed.XAppendingPackedLongBuffer;
import org.elasticsearch.index.fielddata.ordinals.Ordinals.Docs.Iter;

/**
 * {@link Ordinals} implementation which is efficient at storing field data ordinals for multi-valued or sparse fields.
 */
public class MultiOrdinals implements Ordinals {

    private static final int OFFSETS_PAGE_SIZE = 1024;
    private static final int OFFSET_INIT_PAGE_COUNT = 16;

    private final boolean multiValued;
    private final long numOrds;
    private final MonotonicAppendingLongBuffer endOffsets;
    private final AppendingDirect8LongBuffer ords;
    private final int bulkThreshold;

    public MultiOrdinals(OrdinalsBuilder builder, float acceptableOverheadRatio) {
        multiValued = builder.getNumMultiValuesDocs() > 0;
        numOrds = builder.getNumOrds();
        endOffsets = new MonotonicAppendingLongBuffer();
        ords = new AppendingDirect8LongBuffer(OFFSET_INIT_PAGE_COUNT, OFFSETS_PAGE_SIZE, acceptableOverheadRatio);

        long lastEndOffset = 0;
        for (int i = 0; i < builder.maxDoc(); ++i) {
            final LongsRef docOrds = builder.docOrds(i);
            final long endOffset = lastEndOffset + docOrds.length;
            endOffsets.add(endOffset);
            for (int j = 0; j < docOrds.length; ++j) {
                ords.add(docOrds.longs[docOrds.offset + j] - 1);
            }
            lastEndOffset = endOffset;
        }

        ords.freeze();

        int expectedBits = PackedInts.fastestFormatAndBits(OFFSETS_PAGE_SIZE, PackedInts.bitsRequired(numOrds), acceptableOverheadRatio).bitsPerValue;
        if (expectedBits % 8 == 0) {
            // bulk read are effective from 4 elements and up
            bulkThreshold = 4;
        } else {
            // bulk reads are only effective from 64 elements and up
            bulkThreshold = 64;
        }

        assert endOffsets.size() == builder.maxDoc();
        assert ords.size() == builder.getTotalNumOrds() : ords.size() + " != " + builder.getTotalNumOrds();
    }

    @Override
    public boolean hasSingleArrayBackingStorage() {
        return false;
    }

    @Override
    public Object getBackingStorage() {
        return null;
    }

    @Override
    public long getMemorySizeInBytes() {
        return endOffsets.ramBytesUsed() + ords.ramBytesUsed();
    }

    @Override
    public boolean isMultiValued() {
        return multiValued;
    }

    @Override
    public int getNumDocs() {
        return (int) endOffsets.size();
    }

    @Override
    public long getNumOrds() {
        return numOrds;
    }

    @Override
    public long getMaxOrd() {
        return numOrds + 1;
    }

    @Override
    public Ordinals.Docs ordinals() {
        return new MultiDocs(this, bulkThreshold);
    }

    static class MultiDocs implements Ordinals.Docs {

        private final MultiOrdinals ordinals;
        private final MonotonicAppendingLongBuffer endOffsets;
        private final AppendingDirect8LongBuffer ords;
        private final LongsRef longsScratch;
        private final XAppendingPackedLongBuffer.Iter iter;
        private final long bulkThreshold;

        MultiDocs(MultiOrdinals ordinals, long bulkThreshold) {
            this.ordinals = ordinals;
            this.endOffsets = ordinals.endOffsets;
            this.ords = ordinals.ords;
            this.longsScratch = new LongsRef(16);
            this.bulkThreshold = bulkThreshold;
            this.iter = new XAppendingPackedLongBuffer.Iter(ords);
        }

        @Override
        public Ordinals ordinals() {
            return null;
        }

        @Override
        public int getNumDocs() {
            return ordinals.getNumDocs();
        }

        @Override
        public long getNumOrds() {
            return ordinals.getNumOrds();
        }

        @Override
        public long getMaxOrd() {
            return ordinals.getMaxOrd();
        }

        @Override
        public boolean isMultiValued() {
            return ordinals.isMultiValued();
        }

        @Override
        public long getOrd(int docId) {
            final long startOffset = docId > 0 ? endOffsets.get(docId - 1) : 0;
            final long endOffset = endOffsets.get(docId);
            if (startOffset == endOffset) {
                return 0L; // ord for missing values
            } else {
                return 1L + ords.get(startOffset);
            }
        }

        @Override
        public LongsRef getOrds(int docId) {
            final long startOffset = docId > 0 ? endOffsets.get(docId - 1) : 0;
            final long endOffset = endOffsets.get(docId);
            final int numValues = (int) (endOffset - startOffset);
            if (longsScratch.length < numValues) {
                longsScratch.longs = new long[ArrayUtil.oversize(numValues, RamUsageEstimator.NUM_BYTES_LONG)];
            }
            for (int i = 0; i < numValues; ++i) {
                longsScratch.longs[i] = 1L + ords.get(startOffset + i);
            }
            longsScratch.offset = 0;
            longsScratch.length = numValues;
            return longsScratch;
        }

        @Override
        public Iter getIter(int docId) {
            final long startOffset = docId > 0 ? endOffsets.get(docId - 1) : 0;
            final long endOffset = endOffsets.get(docId);
            iter.reset(startOffset, endOffset);
            return iter;
//            // only use bulk is faster for 4 items and more, because it's slower for small reads.
//            if (endOffset - startOffset > bulkThreshold) {
//                iterBulkRead.reset(startOffset, endOffset);
//                return iterBulkRead;
//            } else {
//                iterSingleRead.reset(startOffset, endOffset);
//                return iterSingleRead;
//            }
        }

    }

    /**
     * Reads from underling buffer using single gets. Faster then bulk for small number of items.
     */
    static class MultiIterSingleRead implements Iter {

        final XAppendingPackedLongBuffer ordinals;
        long lStartOffset, lEndOffset;

        MultiIterSingleRead(XAppendingPackedLongBuffer ordinals) {
            this.ordinals = ordinals;
        }

        public void reset(long startOffset, long endOffset) {
            this.lStartOffset = startOffset;
            this.lEndOffset = endOffset;
        }


        @Override
        public long next() {
            if (lStartOffset >= lEndOffset) {
                // really done..
                return 0L;
            }
            return 1L + ordinals.get(lStartOffset++);
        }

    }

    /**
     * Reads from underlying buffer in bulk.
     */
    static class MultiIterBulkRead implements Iter {

        final XAppendingPackedLongBuffer ordinals;
        long lStartOffset, lEndOffset;
        long[] currentOridnals;
        int iOffset;
        int iEndOffset;

        static final int BUFFER_SIZE = 128;

        MultiIterBulkRead(XAppendingPackedLongBuffer ordinals) {
            this.ordinals = ordinals;
            currentOridnals = new long[BUFFER_SIZE];
        }

        public void reset(long startOffset, long endOffset) {
            this.lStartOffset = startOffset;
            this.lEndOffset = endOffset;
            iOffset = iEndOffset = 0; // next will initialize array
        }


        @Override
        public long next() {
            if (iOffset >= iEndOffset) {
                if (lStartOffset >= lEndOffset) {
                    // really done..
                    return 0L;
                } else {
                    iEndOffset = ordinals.get(lStartOffset, currentOridnals, 0, Math.min((int) (lEndOffset - lStartOffset), BUFFER_SIZE));
                    lStartOffset += iEndOffset;
                    iOffset = 0;
                }

            }
            return 1L + currentOridnals[iOffset++];
        }

    }

}
