/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
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
package org.elasticsearch.xpack.prelert.lists;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.elasticsearch.action.support.ToXContentToBytes;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParseFieldMatcherSupplier;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.XContentBuilder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ListDocument extends ToXContentToBytes implements Writeable {
    public static final ParseField TYPE = new ParseField("list");
    public static final ParseField ID = new ParseField("id");
    public static final ParseField ITEMS = new ParseField("items");

    @SuppressWarnings("unchecked")
    public static final ConstructingObjectParser<ListDocument, ParseFieldMatcherSupplier> PARSER = new ConstructingObjectParser<>(
            TYPE.getPreferredName(), a -> new ListDocument((String) a[0], (List<String>) a[1]));

    static {
        PARSER.declareString(ConstructingObjectParser.constructorArg(), ID);
        PARSER.declareStringArray(ConstructingObjectParser.constructorArg(), ITEMS);
    }

    private final String id;
    private final List<String> items;

    @JsonCreator
    public ListDocument(@JsonProperty(value = "id", required = true) String id, @JsonProperty("items") List<String> items) {
        this.id = Objects.requireNonNull(id, ID.getPreferredName() + " must not be null");
        this.items = Objects.requireNonNull(items, ITEMS.getPreferredName() + " must not be null");
    }

    public ListDocument(StreamInput in) throws IOException {
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

        if (!(obj instanceof ListDocument)) {
            return false;
        }

        ListDocument other = (ListDocument) obj;
        return id.equals(other.id) && items.equals(other.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, items);
    }
}