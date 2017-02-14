/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */

package org.elasticsearch.xpack.security.authz.accesscontrol;

import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.FilterDirectoryReader;
import org.apache.lucene.index.FilterLeafReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.PointValues;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.index.StoredFieldVisitor;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.FilterIterator;
import org.apache.lucene.util.automaton.CharacterRunAutomaton;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.logging.LoggerMessageFormat;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.mapper.FieldNamesFieldMapper;
import org.elasticsearch.index.mapper.SourceFieldMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A {@link FilterLeafReader} that exposes only a subset
 * of fields from the underlying wrapped reader.
 */
// based on lucene/test-framework's FieldFilterLeafReader.
public final class FieldSubsetReader extends FilterLeafReader {

    /**
     * Wraps a provided DirectoryReader, exposing a subset of fields.
     * <p>
     * Note that for convenience, the returned reader
     * can be used normally (e.g. passed to {@link DirectoryReader#openIfChanged(DirectoryReader)})
     * and so on.
     * @param in reader to filter
     * @param filter fields to filter.
     */
    public static DirectoryReader wrap(DirectoryReader in, CharacterRunAutomaton filter) throws IOException {
        return new FieldSubsetDirectoryReader(in, filter);
    }

    // wraps subreaders with fieldsubsetreaders.
    static class FieldSubsetDirectoryReader extends FilterDirectoryReader {

        private final CharacterRunAutomaton filter;

        FieldSubsetDirectoryReader(DirectoryReader in, final CharacterRunAutomaton filter) throws IOException {
            super(in, new FilterDirectoryReader.SubReaderWrapper() {
                @Override
                public LeafReader wrap(LeafReader reader) {
                    return new FieldSubsetReader(reader, filter);
                }
            });
            this.filter = filter;
            verifyNoOtherFieldSubsetDirectoryReaderIsWrapped(in);
        }

        @Override
        protected DirectoryReader doWrapDirectoryReader(DirectoryReader in) throws IOException {
            return new FieldSubsetDirectoryReader(in, filter);
        }

        /** Return the automaton that is used to filter fields. */
        CharacterRunAutomaton getFilter() {
            return filter;
        }

        private static void verifyNoOtherFieldSubsetDirectoryReaderIsWrapped(DirectoryReader reader) {
            if (reader instanceof FilterDirectoryReader) {
                FilterDirectoryReader filterDirectoryReader = (FilterDirectoryReader) reader;
                if (filterDirectoryReader instanceof FieldSubsetDirectoryReader) {
                    throw new IllegalArgumentException(LoggerMessageFormat.format("Can't wrap [{}] twice",
                            FieldSubsetDirectoryReader.class));
                } else {
                    verifyNoOtherFieldSubsetDirectoryReaderIsWrapped(filterDirectoryReader.getDelegate());
                }
            }
        }

        @Override
        public Object getCoreCacheKey() {
            return in.getCoreCacheKey();
        }
    }

    /** List of filtered fields */
    private final FieldInfos fieldInfos;
    /** An automaton that only accepts authorized fields. */
    private final CharacterRunAutomaton filter;

    /**
     * Wrap a single segment, exposing a subset of its fields.
     */
    FieldSubsetReader(LeafReader in, CharacterRunAutomaton filter) {
        super(in);
        ArrayList<FieldInfo> filteredInfos = new ArrayList<>();
        for (FieldInfo fi : in.getFieldInfos()) {
            if (filter.run(fi.name)) {
                filteredInfos.add(fi);
            }
        }
        fieldInfos = new FieldInfos(filteredInfos.toArray(new FieldInfo[filteredInfos.size()]));
        this.filter = filter;
    }

    /** returns true if this field is allowed. */
    boolean hasField(String field) {
        return fieldInfos.fieldInfo(field) != null;
    }

    @Override
    public FieldInfos getFieldInfos() {
        return fieldInfos;
    }

    @Override
    public Fields getTermVectors(int docID) throws IOException {
        Fields f = super.getTermVectors(docID);
        if (f == null) {
            return null;
        }
        f = new FieldFilterFields(f);
        // we need to check for emptyness, so we can return null:
        return f.iterator().hasNext() ? f : null;
    }

    /** Filter a map by a {@link CharacterRunAutomaton} that defines the fields to retain. */
    static Map<String, Object> filter(Map<String, ?> map, CharacterRunAutomaton includeAutomaton, int initialState) {
        Map<String, Object> filtered = new HashMap<>();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            String key = entry.getKey();

            int state = step(includeAutomaton, key, initialState);
            if (state == -1) {
                continue;
            }

            Object value = entry.getValue();

            if (value instanceof Map) {
                state = includeAutomaton.step(state, '.');
                if (state == -1) {
                    continue;
                }

                Map<String, ?> mapValue = (Map<String, ?>) value;
                Map<String, Object> filteredValue = filter(mapValue, includeAutomaton, state);
                if (filteredValue.isEmpty() == false) {
                    filtered.put(key, filteredValue);
                }
            } else if (value instanceof Iterable) {
                Iterable<?> iterableValue = (Iterable<?>) value;
                List<Object> filteredValue = filter(iterableValue, includeAutomaton, state);
                if (filteredValue.isEmpty() == false) {
                    filtered.put(key, filteredValue);
                }
            } else if (includeAutomaton.isAccept(state)) {
                filtered.put(key, value);
            }
        }
        return filtered;
    }

    /** Filter a list by a {@link CharacterRunAutomaton} that defines the fields to retain. */
    private static List<Object> filter(Iterable<?> iterable, CharacterRunAutomaton includeAutomaton, int initialState) {
        List<Object> filtered = new ArrayList<>();
        for (Object value : iterable) {
            if (value instanceof Map) {
                int state = includeAutomaton.step(initialState, '.');
                if (state == -1) {
                    continue;
                }
                Map<String, Object> filteredValue = filter((Map<String, ?>)value, includeAutomaton, state);
                if (filteredValue.isEmpty() == false) {
                    filtered.add(filteredValue);
                }
            } else if (value instanceof Iterable) {
                List<Object> filteredValue = filter((Iterable<?>) value, includeAutomaton, initialState);
                if (filteredValue.isEmpty() == false) {
                    filtered.add(filteredValue);
                }
            } else if (includeAutomaton.isAccept(initialState)) {
                filtered.add(value);
            }
        }
        return filtered;
    }

    /** Step through all characters of the provided string, and return the
     *  resulting state, or -1 if that did not lead to a valid state. */
    private static int step(CharacterRunAutomaton automaton, String key, int state) {
        for (int i = 0; state != -1 && i < key.length(); ++i) {
            state = automaton.step(state, key.charAt(i));
        }
        return state;
    }

    @Override
    public void document(final int docID, final StoredFieldVisitor visitor) throws IOException {
        super.document(docID, new StoredFieldVisitor() {
            @Override
            public void binaryField(FieldInfo fieldInfo, byte[] value) throws IOException {
                if (SourceFieldMapper.NAME.equals(fieldInfo.name)) {
                    // for _source, parse, filter out the fields we care about, and serialize back downstream
                    BytesReference bytes = new BytesArray(value);
                    Tuple<XContentType, Map<String, Object>> result = XContentHelper.convertToMap(bytes, true);
                    Map<String, Object> transformedSource = filter(result.v2(), filter, 0);
                    XContentBuilder xContentBuilder = XContentBuilder.builder(result.v1().xContent()).map(transformedSource);
                    visitor.binaryField(fieldInfo, BytesReference.toBytes(xContentBuilder.bytes()));
                } else {
                    visitor.binaryField(fieldInfo, value);
                }
            }

            @Override
            public void stringField(FieldInfo fieldInfo, byte[] value) throws IOException {
                visitor.stringField(fieldInfo, value);
            }

            @Override
            public void intField(FieldInfo fieldInfo, int value) throws IOException {
                visitor.intField(fieldInfo, value);
            }

            @Override
            public void longField(FieldInfo fieldInfo, long value) throws IOException {
                visitor.longField(fieldInfo, value);
            }

            @Override
            public void floatField(FieldInfo fieldInfo, float value) throws IOException {
                visitor.floatField(fieldInfo, value);
            }

            @Override
            public void doubleField(FieldInfo fieldInfo, double value) throws IOException {
                visitor.doubleField(fieldInfo, value);
            }

            @Override
            public Status needsField(FieldInfo fieldInfo) throws IOException {
                return hasField(fieldInfo.name) ? visitor.needsField(fieldInfo) : Status.NO;
            }
        });
    }

    @Override
    public Fields fields() throws IOException {
        return new FieldFilterFields(super.fields());
    }

    @Override
    public NumericDocValues getNumericDocValues(String field) throws IOException {
        return hasField(field) ? super.getNumericDocValues(field) : null;
    }

    @Override
    public BinaryDocValues getBinaryDocValues(String field) throws IOException {
        return hasField(field) ? super.getBinaryDocValues(field) : null;
    }

    @Override
    public SortedDocValues getSortedDocValues(String field) throws IOException {
        return hasField(field) ? super.getSortedDocValues(field) : null;
    }

    @Override
    public SortedNumericDocValues getSortedNumericDocValues(String field) throws IOException {
        return hasField(field) ? super.getSortedNumericDocValues(field) : null;
    }

    @Override
    public SortedSetDocValues getSortedSetDocValues(String field) throws IOException {
        return hasField(field) ? super.getSortedSetDocValues(field) : null;
    }

    @Override
    public NumericDocValues getNormValues(String field) throws IOException {
        return hasField(field) ? super.getNormValues(field) : null;
    }

    @Override
    public Bits getDocsWithField(String field) throws IOException {
        return hasField(field) ? super.getDocsWithField(field) : null;
    }

    // we share core cache keys (for e.g. fielddata)

    @Override
    public Object getCoreCacheKey() {
        return in.getCoreCacheKey();
    }

    /**
     * Filters the Fields instance from the postings.
     * <p>
     * In addition to only returning fields allowed in this subset,
     * the ES internal _field_names (used by exists filter) has special handling,
     * to hide terms for fields that don't exist.
     */
    class FieldFilterFields extends FilterFields {

        FieldFilterFields(Fields in) {
            super(in);
        }

        @Override
        public int size() {
            // this information is not cheap, return -1 like MultiFields does:
            return -1;
        }

        @Override
        public Iterator<String> iterator() {
            return new FilterIterator<String, String>(super.iterator()) {
                @Override
                protected boolean predicateFunction(String field) {
                    return hasField(field);
                }
            };
        }

        @Override
        public Terms terms(String field) throws IOException {
            if (!hasField(field)) {
                return null;
            } else if (FieldNamesFieldMapper.NAME.equals(field)) {
                // for the _field_names field, fields for the document
                // are encoded as postings, where term is the field.
                // so we hide terms for fields we filter out.
                Terms terms = super.terms(field);
                if (terms != null) {
                    // check for null, in case term dictionary is not a ghostbuster
                    // So just because its in fieldinfos and "indexed=true" doesn't mean you can go grab a Terms for it.
                    // It just means at one point there was a document with that field indexed...
                    // The fields infos isn't updates/removed even if no docs refer to it
                    terms = new FieldNamesTerms(terms);
                }
                return terms;
            } else {
                return super.terms(field);
            }
        }
    }

    /**
     * Terms impl for _field_names (used by exists filter) that filters out terms
     * representing fields that should not be visible in this reader.
     */
    class FieldNamesTerms extends FilterTerms {

        FieldNamesTerms(Terms in) {
            super(in);
        }

        @Override
        public TermsEnum iterator() throws IOException {
            return new FieldNamesTermsEnum(in.iterator());
        }

        // we don't support field statistics (since we filter out terms)
        // but this isn't really a big deal: _field_names is not used for ranking.

        @Override
        public int getDocCount() throws IOException {
            return -1;
        }

        @Override
        public long getSumDocFreq() throws IOException {
            return -1;
        }

        @Override
        public long getSumTotalTermFreq() throws IOException {
            return -1;
        }

        @Override
        public long size() throws IOException {
            return -1;
        }
    }

    /**
     * TermsEnum impl for _field_names (used by exists filter) that filters out terms
     * representing fields that should not be visible in this reader.
     */
    class FieldNamesTermsEnum extends FilterTermsEnum {

        FieldNamesTermsEnum(TermsEnum in) {
            super(in);
        }

        /** Return true if term is accepted (matches a field name in this reader). */
        boolean accept(BytesRef term) {
            return hasField(term.utf8ToString());
        }

        @Override
        public boolean seekExact(BytesRef term) throws IOException {
            return accept(term) && in.seekExact(term);
        }

        @Override
        public SeekStatus seekCeil(BytesRef term) throws IOException {
            SeekStatus status = in.seekCeil(term);
            if (status == SeekStatus.END || accept(term())) {
                return status;
            }
            return next() == null ? SeekStatus.END : SeekStatus.NOT_FOUND;
        }

        @Override
        public BytesRef next() throws IOException {
            BytesRef next;
            while ((next = in.next()) != null) {
                if (accept(next)) {
                    break;
                }
            }
            return next;
        }

        // we don't support ordinals, but _field_names is not used in this way

        @Override
        public void seekExact(long ord) throws IOException {
          throw new UnsupportedOperationException();
        }

        @Override
        public long ord() throws IOException {
          throw new UnsupportedOperationException();
        }
    }

    /**
     * Filters the PointValues instance.
     * <p>
     * Like other parts of the index, fields without access will not exist.
     */
    // TODO: push up a similar class into lucene
    // TODO: fix the FieldFilterReader we use in lucene tests!
    final class FieldFilterPointValues extends PointValues {
        private final PointValues in;

        FieldFilterPointValues(PointValues in) {
            this.in = in;
        }

        @Override
        public void intersect(String fieldName, IntersectVisitor visitor) throws IOException {
            if (hasField(fieldName)) {
                in.intersect(fieldName, visitor);
            } else {
                return; // behave as field does not exist
            }
        }

        @Override
        public long estimatePointCount(String fieldName, IntersectVisitor visitor) {
            if (hasField(fieldName)) {
                return in.estimatePointCount(fieldName, visitor);
            } else {
                return 0L; // behave as field does not exist
            }
        }

        @Override
        public byte[] getMinPackedValue(String fieldName) throws IOException {
            if (hasField(fieldName)) {
                return in.getMinPackedValue(fieldName);
            } else {
                return null; // behave as field does not exist
            }
        }

        @Override
        public byte[] getMaxPackedValue(String fieldName) throws IOException {
            if (hasField(fieldName)) {
                return in.getMaxPackedValue(fieldName);
            } else {
                return null; // behave as field does not exist
            }
        }

        @Override
        public int getNumDimensions(String fieldName) throws IOException {
            if (hasField(fieldName)) {
                return in.getNumDimensions(fieldName);
            } else {
                return 0; // behave as field does not exist
            }
        }

        @Override
        public int getBytesPerDimension(String fieldName) throws IOException {
            if (hasField(fieldName)) {
                return in.getBytesPerDimension(fieldName);
            } else {
                return 0; // behave as field does not exist
            }
        }

        @Override
        public long size(String fieldName) {
            if (hasField(fieldName)) {
                return in.size(fieldName);
            } else {
                return 0; // behave as field does not exist
            }
        }

        @Override
        public int getDocCount(String fieldName) {
            if (hasField(fieldName)) {
                return in.getDocCount(fieldName);
            } else {
                return 0;  // behave as field does not exist
            }
        }
    }

    @Override
    public PointValues getPointValues() {
        PointValues points = super.getPointValues();
        if (points == null) {
            return null;
        } else {
            return new FieldFilterPointValues(points);
        }
    }
}
