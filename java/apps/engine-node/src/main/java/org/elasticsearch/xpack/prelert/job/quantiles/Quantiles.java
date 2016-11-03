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
package org.elasticsearch.xpack.prelert.job.quantiles;

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
import java.util.Date;
import java.util.Objects;

/**
 * Quantiles Result POJO
 */
public class Quantiles extends ToXContentToBytes implements Writeable
{
    public static final String QUANTILES_ID = "hierarchical";

    /**
     * Field Names
     */
    public static final ParseField TIMESTAMP = new ParseField("timestamp");
    public static final ParseField QUANTILE_STATE = new ParseField("quantileState");

    /**
     * Elasticsearch type
     */
    public static final ParseField TYPE = new ParseField("quantiles");

    public static final ConstructingObjectParser<Quantiles, ParseFieldMatcherSupplier> PARSER = new ConstructingObjectParser<>(
            TYPE.getPreferredName(), a -> new Quantiles((Date) a[0], (String) a[1]));

    static {
        PARSER.declareField(ConstructingObjectParser.constructorArg(), p -> new Date(p.longValue()), TIMESTAMP, ValueType.LONG);
        PARSER.declareString(ConstructingObjectParser.constructorArg(), QUANTILE_STATE);
    }

    private Date timestamp;
    private String quantileState;

    // NORELEASE remove this constructor when jackson is gone
    public Quantiles() {
    }

    // NORELEASE remove this constructor when jackson is gone
    public void setQuantileState(String quantileState) {
        this.quantileState = quantileState;
    }

    // NORELEASE remove this constructor when jackson is gone
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Quantiles(Date timestamp, String quantilesState) {
        this.timestamp = timestamp;
        quantileState = quantilesState == null ? "" : quantilesState;
    }

    public Quantiles(StreamInput in) throws IOException {
        if (in.readBoolean()) {
            timestamp = new Date(in.readLong());
        }
        quantileState = in.readOptionalString();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        boolean hasTimestamp = timestamp != null;
        out.writeBoolean(hasTimestamp);
        if (hasTimestamp) {
            out.writeLong(timestamp.getTime());
        }
        out.writeOptionalString(quantileState);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        if (timestamp != null) {
            builder.field(TIMESTAMP.getPreferredName(), timestamp.getTime());
        }
        if (quantileState != null) {
            builder.field(QUANTILE_STATE.getPreferredName(), quantileState);
        }
        builder.endObject();
        return builder;
    }

    public Date getTimestamp()
    {
        return timestamp;
    }

    public String getQuantileState()
    {
        return quantileState;
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(quantileState);
    }

    /**
     * Compare all the fields.
     */
    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (other instanceof Quantiles == false)
        {
            return false;
        }

        Quantiles that = (Quantiles) other;

        return Objects.equals(this.getQuantileState(), that.getQuantileState());
    }
}

