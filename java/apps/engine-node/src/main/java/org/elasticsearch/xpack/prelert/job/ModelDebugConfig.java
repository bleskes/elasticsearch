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

import org.elasticsearch.action.support.ToXContentToBytes;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParseFieldMatcherSupplier;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.ObjectParser.ValueType;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.xpack.prelert.job.messages.Messages;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

public class ModelDebugConfig extends ToXContentToBytes implements Writeable {
    /**
     * Enum of the acceptable output destinations.
     */
    public enum DebugDestination implements Writeable {
        FILE("file"),
        DATA_STORE("data_store");

        private String name;

        DebugDestination(String name) {
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

    private static final double MAX_PERCENTILE = 100.0;

    private static final ParseField TYPE_FIELD = new ParseField("modelDebugConfig");
    private static final ParseField WRITE_TO_FIELD = new ParseField("writeTo");
    private static final ParseField BOUNDS_PERCENTILE_FIELD = new ParseField("boundsPercentile");
    private static final ParseField TERMS_FIELD = new ParseField("terms");

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
        PARSER.declareString(ConstructingObjectParser.optionalConstructorArg(), TERMS_FIELD);
    }

    private final DebugDestination writeTo;
    private final double boundsPercentile;
    private final String terms;

    public ModelDebugConfig(double boundsPercentile, String terms) {
        this(DebugDestination.FILE, boundsPercentile, terms);
    }

    public ModelDebugConfig(DebugDestination writeTo, double boundsPercentile, String terms) {
        if (boundsPercentile < 0.0 || boundsPercentile > MAX_PERCENTILE) {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_MODEL_DEBUG_CONFIG_INVALID_BOUNDS_PERCENTILE);
            throw new IllegalArgumentException(msg);
        }
        this.writeTo = writeTo;
        this.boundsPercentile = boundsPercentile;
        this.terms = terms;
    }

    public ModelDebugConfig(StreamInput in) throws IOException {
        writeTo = in.readOptionalWriteable(DebugDestination::readFromStream);
        boundsPercentile = in.readDouble();
        terms = in.readOptionalString();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeOptionalWriteable(writeTo);
        out.writeDouble(boundsPercentile);
        out.writeOptionalString(terms);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        if (writeTo != null) {
            builder.field(WRITE_TO_FIELD.getPreferredName(), writeTo.getName());
        }
        builder.field(BOUNDS_PERCENTILE_FIELD.getPreferredName(), boundsPercentile);
        if (terms != null) {
            builder.field(TERMS_FIELD.getPreferredName(), terms);
        }
        builder.endObject();
        return builder;
    }

    public DebugDestination getWriteTo() {
        return this.writeTo;
    }

    public double getBoundsPercentile() {
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
