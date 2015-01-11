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

package org.elasticsearch.index.mapper.internal;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexOptions;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.fielddata.FieldDataType;
import org.elasticsearch.index.mapper.*;
import org.elasticsearch.index.mapper.core.AbstractFieldMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/** Mapper for the _version field. */
public class SeqNoFieldMapper extends AbstractFieldMapper<byte[]> implements InternalMapper, RootMapper {

    public static final String NAME = "_seq_no";
    public static final String NAME_TERM = NAME + "_term";
    public static final String NAME_COUNTER = NAME + "_counter";

    public static class Defaults {
        public static final String NAME = SeqNoFieldMapper.NAME;
        public static final float BOOST = 1.0f;
        public static final FieldType FIELD_TYPE = new FieldType();
        static {
            FIELD_TYPE.setIndexOptions(IndexOptions.NONE); // not indexed for now
            FIELD_TYPE.setStored(true);
            FIELD_TYPE.setOmitNorms(true);
            FIELD_TYPE.setDocValuesType(DocValuesType.NUMERIC);
            FIELD_TYPE.freeze();
        }

    }

    public static class Builder extends Mapper.Builder<Builder, SeqNoFieldMapper> {

        public Builder() {
            super(Defaults.NAME);
        }

        @Override
        public SeqNoFieldMapper build(BuilderContext context) {
            return new SeqNoFieldMapper();
        }

    }

    public static class TypeParser implements Mapper.TypeParser {
        @Override
        public Mapper.Builder<?, ?> parse(String name, Map<String, Object> node, ParserContext parserContext) throws MapperParsingException {
            Builder builder = new Builder();
            return builder;
        }
    }

    private final ThreadLocal<Tuple<Field, Field>> fieldCache = new ThreadLocal<Tuple<Field, Field>>() {
        @Override
        protected Tuple<Field, Field> initialValue() {
            return new Tuple<Field, Field>(new NumericDocValuesField(NAME_TERM, -1L), new NumericDocValuesField(NAME_COUNTER, -1L));
        }
    };

    public SeqNoFieldMapper() {
        super(new Names(NAME, NAME, NAME, NAME), Defaults.BOOST, Defaults.FIELD_TYPE, null, null, null, null, null, null, null, null, ImmutableSettings.EMPTY);
    }

    @Override
    public void preParse(ParseContext context) throws IOException {
        super.parse(context);
    }

    @Override
    protected void parseCreateField(ParseContext context, List<Field> fields) throws IOException {
        // see UidFieldMapper.parseCreateField
        final Tuple<Field, Field> seqNo = fieldCache.get();
        fields.add(seqNo.v1());
        fields.add(seqNo.v2());
        context.sequenceNo(seqNo);
    }

    @Override
    public void parse(ParseContext context) throws IOException {
        // _version added in preparse
    }

    @Override
    public byte[] value(Object value) {
        throw new UnsupportedOperationException("meh");
    }

    @Override
    public void postParse(ParseContext context) throws IOException {
    }

    @Override
    public boolean includeInObject() {
        return false;
    }

    @Override
    public FieldType defaultFieldType() {
        return Defaults.FIELD_TYPE;
    }

    @Override
    public FieldDataType defaultFieldDataType() {
        return new FieldDataType("long");
    }

    @Override
    protected String contentType() {
        return "no_commit";
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        boolean includeDefaults = params.paramAsBoolean("include_defaults", false);

        if (!includeDefaults && (docValuesFormat == null || docValuesFormat.name().equals(defaultDocValuesFormat()))) {
            return builder;
        }

        builder.startObject("_seq_no");

        builder.endObject();
        return builder;
    }

    @Override
    public void merge(Mapper mergeWith, MergeContext mergeContext) throws MergeMappingException {
    }

    @Override
    public void close() {
        fieldCache.remove();
    }

    @Override
    public boolean hasDocValues() {
        return true;
    }
}
