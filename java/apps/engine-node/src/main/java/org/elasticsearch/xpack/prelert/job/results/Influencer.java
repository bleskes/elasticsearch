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

import org.elasticsearch.action.support.ToXContentToBytes;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParseFieldMatcherSupplier;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.ObjectParser.ValueType;
import org.elasticsearch.common.xcontent.XContentParser.Token;
import org.elasticsearch.xpack.prelert.utils.time.TimeUtils;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Date;
import java.util.Objects;

public class Influencer extends ToXContentToBytes implements Writeable {
    /**
     * Elasticsearch type
     */
    public static final ParseField TYPE = new ParseField("influencer");

    /*
     * Field names
     */
    public static final ParseField PROBABILITY = new ParseField("probability");
    public static final ParseField TIMESTAMP = new ParseField("timestamp");
    public static final ParseField INFLUENCER_FIELD_NAME = new ParseField("influencerFieldName");
    public static final ParseField INFLUENCER_FIELD_VALUE = new ParseField("influencerFieldValue");
    public static final ParseField INITIAL_ANOMALY_SCORE = new ParseField("initialAnomalyScore");
    public static final ParseField ANOMALY_SCORE = new ParseField("anomalyScore");

    public static final ConstructingObjectParser<Influencer, ParseFieldMatcherSupplier> PARSER = new ConstructingObjectParser<>(
            TYPE.getPreferredName(), a -> new Influencer((String) a[0], (String) a[1]));

    static {
        PARSER.declareString(ConstructingObjectParser.constructorArg(), INFLUENCER_FIELD_NAME);
        PARSER.declareString(ConstructingObjectParser.constructorArg(), INFLUENCER_FIELD_VALUE);
        PARSER.declareDouble(Influencer::setProbability, PROBABILITY);
        PARSER.declareDouble(Influencer::setAnomalyScore, ANOMALY_SCORE);
        PARSER.declareDouble(Influencer::setInitialAnomalyScore, INITIAL_ANOMALY_SCORE);
        PARSER.declareBoolean(Influencer::setInterim, Bucket.IS_INTERIM);
        PARSER.declareField(Influencer::setTimestamp, p -> {
            if (p.currentToken() == Token.VALUE_NUMBER) {
                return new Date(p.longValue());
            } else if (p.currentToken() == Token.VALUE_STRING) {
                return new Date(TimeUtils.dateStringToEpoch(p.text()));
            }
            throw new IllegalArgumentException("unexpected token [" + p.currentToken() + "] for [" + TIMESTAMP.getPreferredName() + "]");
        }, TIMESTAMP, ValueType.VALUE);
    }

    private String id;

    private Date timestamp;
    private String influenceField;
    private String influenceValue;
    private double probability;
    private double initialAnomalyScore;
    private double anomalyScore;
    private boolean hadBigNormalisedUpdate;
    private boolean isInterim;

    // NORELEASE remove me when InfluencerParser is gone
    public Influencer() {
    }

    public Influencer(String fieldName, String fieldValue) {
        influenceField = fieldName;
        influenceValue = fieldValue;
    }

    public Influencer(StreamInput in) throws IOException {
        id = in.readOptionalString();
        if (in.readBoolean()) {
            timestamp = new Date(in.readLong());
        }
        influenceField = in.readString();
        influenceValue = in.readString();
        probability = in.readDouble();
        initialAnomalyScore = in.readDouble();
        anomalyScore = in.readDouble();
        hadBigNormalisedUpdate = in.readBoolean();
        isInterim = in.readBoolean();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeOptionalString(id);
        boolean hasTimestamp = timestamp != null;
        out.writeBoolean(hasTimestamp);
        if (hasTimestamp) {
            out.writeLong(timestamp.getTime());
        }
        out.writeString(influenceField);
        out.writeString(influenceValue);
        out.writeDouble(probability);
        out.writeDouble(initialAnomalyScore);
        out.writeDouble(anomalyScore);
        out.writeBoolean(hadBigNormalisedUpdate);
        out.writeBoolean(isInterim);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(INFLUENCER_FIELD_NAME.getPreferredName(), influenceField);
        builder.field(INFLUENCER_FIELD_VALUE.getPreferredName(), influenceValue);
        builder.field(ANOMALY_SCORE.getPreferredName(), anomalyScore);
        builder.field(INITIAL_ANOMALY_SCORE.getPreferredName(), initialAnomalyScore);
        builder.field(PROBABILITY.getPreferredName(), probability);
        builder.field(Bucket.IS_INTERIM.getPreferredName(), isInterim);
        if (timestamp != null) {
            builder.field(TIMESTAMP.getPreferredName(), timestamp.getTime());
        }
        builder.endObject();
        return builder;
    }

    /**
     * Data store ID of this record. May be null for records that have not been
     * read from the data store.
     */
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getProbability() {
        return probability;
    }

    // NORELEASE remove me when InfluencerParser is gone
    public void setProbability(double probability) {
        this.probability = probability;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date date) {
        timestamp = date;
    }

    public String getInfluencerFieldName() {
        return influenceField;
    }

    // NORELEASE remove me when InfluencerParser is gone
    public void setInfluencerFieldName(String fieldName) {
        influenceField = fieldName;
    }

    public String getInfluencerFieldValue() {
        return influenceValue;
    }

    // NORELEASE remove me when InfluencerParser is gone
    public void setInfluencerFieldValue(String fieldValue) {
        influenceValue = fieldValue;
    }

    public double getInitialAnomalyScore() {
        return initialAnomalyScore;
    }

    public void setInitialAnomalyScore(double influenceScore) {
        initialAnomalyScore = influenceScore;
    }

    public double getAnomalyScore() {
        return anomalyScore;
    }

    public void setAnomalyScore(double score) {
        anomalyScore = score;
    }

    public boolean isInterim() {
        return isInterim;
    }

    public void setInterim(boolean value) {
        isInterim = value;
    }

    public boolean hadBigNormalisedUpdate() {
        return this.hadBigNormalisedUpdate;
    }

    public void resetBigNormalisedUpdateFlag() {
        hadBigNormalisedUpdate = false;
    }

    public void raiseBigNormalisedUpdateFlag() {
        hadBigNormalisedUpdate = true;
    }

    @Override
    public int hashCode() {
        // ID is NOT included in the hash, so that a record from the data store
        // will hash the same as a record representing the same anomaly that did
        // not come from the data store

        // hadBigNormalisedUpdate is also deliberately excluded from the hash

        return Objects.hash(timestamp, influenceField, influenceValue, initialAnomalyScore, anomalyScore, probability, isInterim);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        Influencer other = (Influencer) obj;

        // ID is NOT compared, so that a record from the data store will compare
        // equal to a record representing the same anomaly that did not come
        // from the data store

        // hadBigNormalisedUpdate is also deliberately excluded from the test
        return Objects.equals(timestamp, other.timestamp) && Objects.equals(influenceField, other.influenceField)
                && Objects.equals(influenceValue, other.influenceValue)
                && Double.compare(initialAnomalyScore, other.initialAnomalyScore) == 0
                && Double.compare(anomalyScore, other.anomalyScore) == 0 && Double.compare(probability, other.probability) == 0
                && (isInterim == other.isInterim);
    }
}
