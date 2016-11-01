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
package org.elasticsearch.xpack.prelert.job.results;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import org.elasticsearch.action.support.ToXContentToBytes;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParseFieldMatcherSupplier;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ObjectParser;
import org.elasticsearch.common.xcontent.ObjectParser.ValueType;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.DotNotationReverser;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.StorageSerialisable;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.StorageSerialiser;

import java.io.IOException;
import java.util.Date;
import java.util.Objects;

/**
 * Model Debug POJO.
 * Some of the fields being with the word "debug".  This avoids creation of
 * reserved words that are likely to clash with fields in the input data (due to
 * the restrictions on Elasticsearch mappings).
 */
@JsonIgnoreProperties({"id"})
@JsonInclude(Include.NON_NULL)
public class ModelDebugOutput extends ToXContentToBytes implements Writeable, StorageSerialisable
{
    public static final ParseField TYPE = new ParseField("modelDebugOutput");
    public static final ParseField TIMESTAMP = new ParseField("timestamp");
    public static final ParseField PARTITION_FIELD_NAME = new ParseField("partitionFieldName");
    public static final ParseField PARTITION_FIELD_VALUE = new ParseField("partitionFieldValue");
    public static final ParseField OVER_FIELD_NAME = new ParseField("overFieldName");
    public static final ParseField OVER_FIELD_VALUE = new ParseField("overFieldValue");
    public static final ParseField BY_FIELD_NAME = new ParseField("byFieldName");
    public static final ParseField BY_FIELD_VALUE = new ParseField("byFieldValue");
    public static final ParseField DEBUG_FEATURE = new ParseField("debugFeature");
    public static final ParseField DEBUG_LOWER = new ParseField("debugLower");
    public static final ParseField DEBUG_UPPER = new ParseField("debugUpper");
    public static final ParseField DEBUG_MEDIAN = new ParseField("debugMedian");
    public static final ParseField ACTUAL = new ParseField("actual");

    public static final ObjectParser<ModelDebugOutput, ParseFieldMatcherSupplier> PARSER = new ObjectParser<>(TYPE.getPreferredName(),
            ModelDebugOutput::new);

    static {
        PARSER.declareField(ModelDebugOutput::setTimestamp, p -> new Date(p.longValue()), TIMESTAMP, ValueType.LONG);
        PARSER.declareString(ModelDebugOutput::setPartitionFieldName, PARTITION_FIELD_NAME);
        PARSER.declareString(ModelDebugOutput::setPartitionFieldValue, PARTITION_FIELD_VALUE);
        PARSER.declareString(ModelDebugOutput::setOverFieldName, OVER_FIELD_NAME);
        PARSER.declareString(ModelDebugOutput::setOverFieldValue, OVER_FIELD_VALUE);
        PARSER.declareString(ModelDebugOutput::setByFieldName, BY_FIELD_NAME);
        PARSER.declareString(ModelDebugOutput::setByFieldValue, BY_FIELD_VALUE);
        PARSER.declareString(ModelDebugOutput::setDebugFeature, DEBUG_FEATURE);
        PARSER.declareDouble(ModelDebugOutput::setDebugLower, DEBUG_LOWER);
        PARSER.declareDouble(ModelDebugOutput::setDebugUpper, DEBUG_UPPER);
        PARSER.declareDouble(ModelDebugOutput::setDebugMedian, DEBUG_MEDIAN);
        PARSER.declareDouble(ModelDebugOutput::setActual, ACTUAL);
    }

    private Date timestamp;
    private String id;
    private String partitionFieldName;
    private String partitionFieldValue;
    private String overFieldName;
    private String overFieldValue;
    private String byFieldName;
    private String byFieldValue;
    private String debugFeature;
    private double debugLower;
    private double debugUpper;
    private double debugMedian;
    private double actual;

    public ModelDebugOutput() {
    }

    public ModelDebugOutput(StreamInput in) throws IOException {
        if (in.readBoolean()) {
            timestamp = new Date(in.readLong());
        }
        id = in.readOptionalString();
        partitionFieldName = in.readOptionalString();
        partitionFieldValue = in.readOptionalString();
        overFieldName = in.readOptionalString();
        overFieldValue = in.readOptionalString();
        byFieldName = in.readOptionalString();
        byFieldValue = in.readOptionalString();
        debugFeature = in.readOptionalString();
        debugLower = in.readDouble();
        debugUpper = in.readDouble();
        debugMedian = in.readDouble();
        actual = in.readDouble();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        boolean hasTimestamp = timestamp != null;
        out.writeBoolean(hasTimestamp);
        if (hasTimestamp) {
            out.writeLong(timestamp.getTime());
        }
        out.writeOptionalString(id);
        out.writeOptionalString(partitionFieldName);
        out.writeOptionalString(partitionFieldValue);
        out.writeOptionalString(overFieldName);
        out.writeOptionalString(overFieldValue);
        out.writeOptionalString(byFieldName);
        out.writeOptionalString(byFieldValue);
        out.writeOptionalString(debugFeature);
        out.writeDouble(debugLower);
        out.writeDouble(debugUpper);
        out.writeDouble(debugMedian);
        out.writeDouble(actual);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        if (timestamp != null) {
            builder.field(TIMESTAMP.getPreferredName(), timestamp.getTime());
        }
        if (partitionFieldName != null) {
            builder.field(PARTITION_FIELD_NAME.getPreferredName(), partitionFieldName);
        }
        if (partitionFieldValue != null) {
            builder.field(PARTITION_FIELD_VALUE.getPreferredName(), partitionFieldValue);
        }
        if (overFieldName != null) {
            builder.field(OVER_FIELD_NAME.getPreferredName(), overFieldName);
        }
        if (overFieldValue != null) {
            builder.field(OVER_FIELD_VALUE.getPreferredName(), overFieldValue);
        }
        if (byFieldName != null) {
            builder.field(BY_FIELD_NAME.getPreferredName(), byFieldName);
        }
        if (byFieldValue != null) {
            builder.field(BY_FIELD_VALUE.getPreferredName(), byFieldValue);
        }
        if (debugFeature != null) {
            builder.field(DEBUG_FEATURE.getPreferredName(), debugFeature);
        }
        builder.field(DEBUG_LOWER.getPreferredName(), debugLower);
        builder.field(DEBUG_UPPER.getPreferredName(), debugUpper);
        builder.field(DEBUG_MEDIAN.getPreferredName(), debugMedian);
        builder.field(ACTUAL.getPreferredName(), actual);
        builder.endObject();
        return builder;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public Date getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(Date timestamp)
    {
        this.timestamp = timestamp;
    }

    public String getPartitionFieldName()
    {
        return partitionFieldName;
    }

    public void setPartitionFieldName(String partitionFieldName)
    {
        this.partitionFieldName = partitionFieldName;
    }

    public String getPartitionFieldValue()
    {
        return partitionFieldValue;
    }

    public void setPartitionFieldValue(String partitionFieldValue)
    {
        this.partitionFieldValue = partitionFieldValue;
    }

    public String getOverFieldName()
    {
        return overFieldName;
    }

    public void setOverFieldName(String overFieldName)
    {
        this.overFieldName = overFieldName;
    }

    public String getOverFieldValue()
    {
        return overFieldValue;
    }

    public void setOverFieldValue(String overFieldValue)
    {
        this.overFieldValue = overFieldValue;
    }

    public String getByFieldName()
    {
        return byFieldName;
    }

    public void setByFieldName(String byFieldName)
    {
        this.byFieldName = byFieldName;
    }

    public String getByFieldValue()
    {
        return byFieldValue;
    }

    public void setByFieldValue(String byFieldValue)
    {
        this.byFieldValue = byFieldValue;
    }

    public String getDebugFeature()
    {
        return debugFeature;
    }

    public void setDebugFeature(String debugFeature)
    {
        this.debugFeature = debugFeature;
    }

    public double getDebugLower()
    {
        return debugLower;
    }

    public void setDebugLower(double debugLower)
    {
        this.debugLower = debugLower;
    }

    public double getDebugUpper()
    {
        return debugUpper;
    }

    public void setDebugUpper(double debugUpper)
    {
        this.debugUpper = debugUpper;
    }

    public double getDebugMedian()
    {
        return debugMedian;
    }

    public void setDebugMedian(double debugMedian)
    {
        this.debugMedian = debugMedian;
    }

    public double getActual()
    {
        return actual;
    }

    public void setActual(double actual)
    {
        this.actual = actual;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }
        if (other instanceof ModelDebugOutput == false)
        {
            return false;
        }
        // id excluded here as it is generated by the datastore
        ModelDebugOutput that = (ModelDebugOutput) other;
        return Objects.equals(this.timestamp, that.timestamp) &&
                Objects.equals(this.partitionFieldValue, that.partitionFieldValue) &&
                Objects.equals(this.partitionFieldName, that.partitionFieldName) &&
                Objects.equals(this.overFieldValue, that.overFieldValue) &&
                Objects.equals(this.overFieldName, that.overFieldName) &&
                Objects.equals(this.byFieldValue, that.byFieldValue) &&
                Objects.equals(this.byFieldName, that.byFieldName) &&
                Objects.equals(this.debugFeature, that.debugFeature) &&
                this.debugLower == that.debugLower &&
                this.debugUpper == that.debugUpper &&
                this.debugMedian == that.debugMedian &&
                this.actual == that.actual;
    }

    @Override
    public int hashCode()
    {
        // id excluded here as it is generated by the datastore
        return Objects.hash(timestamp, partitionFieldName, partitionFieldValue,
                overFieldName, overFieldValue, byFieldName, byFieldValue,
                debugFeature, debugLower, debugUpper, debugMedian, actual);
    }

    @Override
    public void serialise(StorageSerialiser serialiser) throws IOException
    {
        serialiser.addTimestamp(timestamp)
        .add(DEBUG_FEATURE.getPreferredName(), debugFeature)
        .add(DEBUG_LOWER.getPreferredName(), debugLower)
        .add(DEBUG_UPPER.getPreferredName(), debugUpper)
        .add(DEBUG_MEDIAN.getPreferredName(), debugMedian)
        .add(ACTUAL.getPreferredName(), actual);

        DotNotationReverser reverser = serialiser.newDotNotationReverser();

        if (byFieldName != null)
        {
            serialiser.add(BY_FIELD_NAME.getPreferredName(), byFieldName);
            if (byFieldValue != null)
            {
                reverser.add(byFieldName, byFieldValue);
            }
        }
        if (byFieldValue != null)
        {
            serialiser.add(BY_FIELD_VALUE.getPreferredName(), byFieldValue);
        }
        if (overFieldName != null)
        {
            serialiser.add(OVER_FIELD_NAME.getPreferredName(), overFieldName);
            if (overFieldValue != null)
            {
                reverser.add(overFieldName, overFieldValue);
            }
        }
        if (overFieldValue != null)
        {
            serialiser.add(OVER_FIELD_VALUE.getPreferredName(), overFieldValue);
        }
        if (partitionFieldName != null)
        {
            serialiser.add(PARTITION_FIELD_NAME.getPreferredName(), partitionFieldName);
            if (partitionFieldValue != null)
            {
                reverser.add(partitionFieldName, partitionFieldValue);
            }
        }
        if (partitionFieldValue != null)
        {
            serialiser.add(PARTITION_FIELD_VALUE.getPreferredName(), partitionFieldValue);
        }

        serialiser.addReverserResults(reverser);
    }
}
