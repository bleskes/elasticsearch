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
package org.elasticsearch.xpack.prelert.job;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.elasticsearch.action.support.ToXContentToBytes;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParseFieldMatcherSupplier;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.ObjectParser.ValueType;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

@JsonIgnoreProperties({ "enabled" })
@JsonInclude(Include.NON_NULL)
public class ModelDebugConfig extends ToXContentToBytes implements Writeable {
    /**
     * Enum of the acceptable output destinations.
     */
    public enum DebugDestination implements Writeable {
        FILE("file"),
        DATA_STORE("data_store");

        private String name;

        private DebugDestination(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        /**
         * Case-insensitive from string method. Works with FILE, File, file,
         * etc.
         *
         * @param value
         *            String representation
         * @return The output destination
         */
        @JsonCreator
        public static DebugDestination forString(String value) {
            String valueUpperCase = value.toUpperCase(Locale.ROOT);
            return DebugDestination.valueOf(valueUpperCase);
        }

        public static DebugDestination readFromStream(StreamInput in) throws IOException {
            int ordinal = in.readVInt();
            if (ordinal < 0 || ordinal >= values().length) {
                throw new IOException("Unknown DebugDestination ordinal [" + ordinal + "]");
            }
            return values()[ordinal];
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeVInt(ordinal());
        }
    }

    public static final ParseField TYPE_FIELD = new ParseField("modelDebugConfig");
    public static final ParseField WRITE_TO_FIELD = new ParseField("writeTo");
    public static final ParseField BOUNDS_PERCENTILE_FIELD = new ParseField("boundsPercentile");
    public static final ParseField TERMS_FIELD = new ParseField("terms");

    public static final ConstructingObjectParser<ModelDebugConfig, ParseFieldMatcherSupplier> PARSER = new ConstructingObjectParser<>(
            TYPE_FIELD.getPreferredName(), a -> {
                if (a[0] == null) {
                    return new ModelDebugConfig((Double) a[1], (String) a[2]);
                } else {
                    return new ModelDebugConfig((DebugDestination) a[0], (Double) a[1], (String) a[2]);
                }
            });
    static {
        PARSER.declareField(ConstructingObjectParser.constructorArg(), p -> DebugDestination.forString(p.text()), WRITE_TO_FIELD,
                ValueType.STRING);
        PARSER.declareDouble(ConstructingObjectParser.constructorArg(), BOUNDS_PERCENTILE_FIELD);
        PARSER.declareString(ConstructingObjectParser.constructorArg(), TERMS_FIELD);
    }

    private DebugDestination writeTo;
    private Double boundsPercentile;
    private String terms;

    public ModelDebugConfig() {
        // NB: this.writeTo defaults to null in this case, otherwise an update
        // to
        // the bounds percentile could switch where the debug is written to
    }

    public ModelDebugConfig(double boundsPercentile, String terms) {
        this.writeTo = DebugDestination.FILE;
        this.boundsPercentile = boundsPercentile;
        this.terms = terms;
    }

    public ModelDebugConfig(DebugDestination writeTo, double boundsPercentile, String terms) {
        this.writeTo = writeTo;
        this.boundsPercentile = boundsPercentile;
        this.terms = terms;
    }

    public ModelDebugConfig(StreamInput in) throws IOException {
        if (in.readBoolean()) {
            writeTo = DebugDestination.readFromStream(in);
        }
        if (in.readBoolean()) {
            boundsPercentile = in.readDouble();
        }
        if (in.readBoolean()) {
            terms = in.readString();
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        boolean hasWriteTo = writeTo != null;
        out.writeBoolean(hasWriteTo);
        if (hasWriteTo) {
            writeTo.writeTo(out);
        }
        boolean hasBounds = boundsPercentile != null;
        out.writeBoolean(hasBounds);
        if (hasBounds) {
            out.writeDouble(boundsPercentile);
        }
        boolean hasTerms = terms != null;
        out.writeBoolean(hasTerms);
        if (hasTerms) {
            out.writeString(terms);
        }
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        if (writeTo != null) {
            builder.field(WRITE_TO_FIELD.getPreferredName(), writeTo.getName());
        }
        if (boundsPercentile != null) {
            builder.field(BOUNDS_PERCENTILE_FIELD.getPreferredName(), boundsPercentile);
        }
        if (terms != null) {
            builder.field(TERMS_FIELD.getPreferredName(), terms);
        }
        builder.endObject();
        return builder;
    }

    public DebugDestination getWriteTo() {
        return this.writeTo;
    }

    public boolean isEnabled() {
        return this.boundsPercentile != null;
    }

    public Double getBoundsPercentile() {
        return this.boundsPercentile;
    }

    public String getTerms() {
        return this.terms;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other instanceof ModelDebugConfig == false) {
            return false;
        }

        ModelDebugConfig that = (ModelDebugConfig) other;
        return Objects.equals(this.writeTo, that.writeTo) && Objects.equals(this.boundsPercentile, that.boundsPercentile)
                && Objects.equals(this.terms, that.terms);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.writeTo, boundsPercentile, terms);
    }
}
