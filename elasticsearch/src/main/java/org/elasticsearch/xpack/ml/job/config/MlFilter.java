/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.ml.job.config;

import org.elasticsearch.action.support.ToXContentToBytes;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MlFilter extends ToXContentToBytes implements Writeable {
    public static final ParseField TYPE = new ParseField("filter");
    public static final ParseField ID = new ParseField("id");
    public static final ParseField ITEMS = new ParseField("items");

    // For QueryPage
    public static final ParseField RESULTS_FIELD = new ParseField("filters");

    @SuppressWarnings("unchecked")
    public static final ConstructingObjectParser<MlFilter, Void> PARSER = new ConstructingObjectParser<>(
            TYPE.getPreferredName(), a -> new MlFilter((String) a[0], (List<String>) a[1]));

    static {
        PARSER.declareString(ConstructingObjectParser.constructorArg(), ID);
        PARSER.declareStringArray(ConstructingObjectParser.constructorArg(), ITEMS);
    }

    private final String id;
    private final List<String> items;

    public MlFilter(String id, List<String> items) {
        this.id = Objects.requireNonNull(id, ID.getPreferredName() + " must not be null");
        this.items = Objects.requireNonNull(items, ITEMS.getPreferredName() + " must not be null");
    }

    public MlFilter(StreamInput in) throws IOException {
        id = in.readString();
        items = Arrays.asList(in.readStringArray());
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(id);
        out.writeStringArray(items.toArray(new String[items.size()]));
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(ID.getPreferredName(), id);
        builder.field(ITEMS.getPreferredName(), items);
        builder.endObject();
        return builder;
    }

    public String getId() {
        return id;
    }

    public List<String> getItems() {
        return new ArrayList<>(items);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof MlFilter)) {
            return false;
        }

        MlFilter other = (MlFilter) obj;
        return id.equals(other.id) && items.equals(other.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, items);
    }
}