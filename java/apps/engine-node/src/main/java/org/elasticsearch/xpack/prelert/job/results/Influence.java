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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.elasticsearch.action.support.ToXContentToBytes;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParseFieldMatcherSupplier;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.StorageSerialisable;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.StorageSerialiser;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Influence field name and list of influence field values/score pairs
 */
public class Influence extends ToXContentToBytes implements Writeable, StorageSerialisable
{
    /**
     * Note all publicly exposed field names are "influencer" not "influence"
     */
    public static final ParseField INFLUENCER = new ParseField("influencer");
    public static final ParseField INFLUENCER_FIELD_NAME = new ParseField("influencerFieldName");
    public static final ParseField INFLUENCER_FIELD_VALUES = new ParseField("influencerFieldValues");

    @SuppressWarnings("unchecked")
    public static final ConstructingObjectParser<Influence, ParseFieldMatcherSupplier> PARSER = new ConstructingObjectParser<>(
            INFLUENCER.getPreferredName(), a -> new Influence((String) a[0], (List<String>) a[1]));

    static {
        PARSER.declareString(ConstructingObjectParser.constructorArg(), INFLUENCER_FIELD_NAME);
        PARSER.declareStringArray(ConstructingObjectParser.constructorArg(), INFLUENCER_FIELD_VALUES);
    }

    private String field;
    private List<String> fieldValues;

    @JsonCreator
    public Influence(@JsonProperty("influencerFieldName") String field,
            @JsonProperty("influencerFieldValues") List<String> fieldValues)
    {
        this.field = field;
        this.fieldValues = fieldValues;
    }

    public Influence(StreamInput in) throws IOException {
        this.field = in.readString();
        this.fieldValues = Arrays.asList(in.readStringArray());
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(field);
        out.writeStringArray(fieldValues.toArray(new String[fieldValues.size()]));
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(INFLUENCER_FIELD_NAME.getPreferredName(), field);
        builder.field(INFLUENCER_FIELD_VALUES.getPreferredName(), fieldValues);
        builder.endObject();
        return builder;
    }

    public String getInfluencerFieldName()
    {
        return field;
    }

    public List<String> getInfluencerFieldValues()
    {
        return fieldValues;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(field, fieldValues);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (obj == null)
        {
            return false;
        }

        if (getClass() != obj.getClass())
        {
            return false;
        }

        Influence other = (Influence) obj;

        return Objects.equals(field, other.field) &&
                Objects.equals(fieldValues, other.fieldValues);
    }

    @Override
    public void serialise(StorageSerialiser serialiser) throws IOException
    {
        serialiser.add(INFLUENCER_FIELD_NAME.getPreferredName(), field).add(INFLUENCER_FIELD_VALUES.getPreferredName(), fieldValues);
    }
}
