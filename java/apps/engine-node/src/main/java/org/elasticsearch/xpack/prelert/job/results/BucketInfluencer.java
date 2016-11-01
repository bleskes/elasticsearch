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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import org.elasticsearch.action.support.ToXContentToBytes;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParseFieldMatcherSupplier;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ObjectParser;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.StorageSerialisable;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.StorageSerialiser;

import java.io.IOException;
import java.util.Objects;

@JsonInclude(Include.NON_NULL)
public class BucketInfluencer extends ToXContentToBytes implements Writeable, StorageSerialisable {
    /**
     * Elasticsearch type
     */
    public static final ParseField TYPE = new ParseField("bucketInfluencer");

    /**
     * This is the field name of the time bucket influencer.
     */
    public static final ParseField BUCKET_TIME = new ParseField("bucketTime");

    /*
     * Field names
     */
    public static final ParseField INFLUENCER_FIELD_NAME = new ParseField("influencerFieldName");
    public static final ParseField INITIAL_ANOMALY_SCORE = new ParseField("initialAnomalyScore");
    public static final ParseField ANOMALY_SCORE = new ParseField("anomalyScore");
    public static final ParseField RAW_ANOMALY_SCORE = new ParseField("rawAnomalyScore");
    public static final ParseField PROBABILITY = new ParseField("probability");

    public static final ObjectParser<BucketInfluencer, ParseFieldMatcherSupplier> PARSER = new ObjectParser<>(TYPE.getPreferredName(),
            BucketInfluencer::new);

    static {
        PARSER.declareString(BucketInfluencer::setInfluencerFieldName, INFLUENCER_FIELD_NAME);
        PARSER.declareDouble(BucketInfluencer::setInitialAnomalyScore, INITIAL_ANOMALY_SCORE);
        PARSER.declareDouble(BucketInfluencer::setAnomalyScore, ANOMALY_SCORE);
        PARSER.declareDouble(BucketInfluencer::setRawAnomalyScore, RAW_ANOMALY_SCORE);
        PARSER.declareDouble(BucketInfluencer::setProbability, PROBABILITY);
    }

    private String influenceField;
    private double initialAnomalyScore;
    private double anomalyScore;
    private double rawAnomalyScore;
    private double probability;

    public BucketInfluencer() {
        // Default constructor
    }

    public BucketInfluencer(StreamInput in) throws IOException {
        influenceField = in.readOptionalString();
        initialAnomalyScore = in.readDouble();
        anomalyScore = in.readDouble();
        rawAnomalyScore = in.readDouble();
        probability = in.readDouble();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeOptionalString(influenceField);
        out.writeDouble(initialAnomalyScore);
        out.writeDouble(anomalyScore);
        out.writeDouble(rawAnomalyScore);
        out.writeDouble(probability);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        if (influenceField != null) {
            builder.field(INFLUENCER_FIELD_NAME.getPreferredName(), influenceField);
        }
        builder.field(INITIAL_ANOMALY_SCORE.getPreferredName(), initialAnomalyScore);
        builder.field(ANOMALY_SCORE.getPreferredName(), anomalyScore);
        builder.field(RAW_ANOMALY_SCORE.getPreferredName(), rawAnomalyScore);
        builder.field(PROBABILITY.getPreferredName(), probability);
        builder.endObject();
        return builder;
    }

    public double getProbability() {
        return probability;
    }

    public void setProbability(double probability) {
        this.probability = probability;
    }

    public String getInfluencerFieldName() {
        return influenceField;
    }

    public void setInfluencerFieldName(String fieldName) {
        this.influenceField = fieldName;
    }

    public double getInitialAnomalyScore() {
        return initialAnomalyScore;
    }

    public void setInitialAnomalyScore(double influenceScore) {
        this.initialAnomalyScore = influenceScore;
    }

    public double getAnomalyScore() {
        return anomalyScore;
    }

    public void setAnomalyScore(double score) {
        anomalyScore = score;
    }

    public double getRawAnomalyScore() {
        return rawAnomalyScore;
    }

    public void setRawAnomalyScore(double score) {
        rawAnomalyScore = score;
    }

    @Override
    public int hashCode() {
        return Objects.hash(influenceField, initialAnomalyScore, anomalyScore, rawAnomalyScore, probability);
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

        BucketInfluencer other = (BucketInfluencer) obj;

        return Objects.equals(influenceField, other.influenceField) && Double.compare(initialAnomalyScore, other.initialAnomalyScore) == 0
                && Double.compare(anomalyScore, other.anomalyScore) == 0 && Double.compare(rawAnomalyScore, other.rawAnomalyScore) == 0
                && Double.compare(probability, other.probability) == 0;
    }

    @Override
    public void serialise(StorageSerialiser serialiser) throws IOException {
        serialiser.add(PROBABILITY.getPreferredName(), probability).add(INFLUENCER_FIELD_NAME.getPreferredName(), influenceField)
        .add(INITIAL_ANOMALY_SCORE.getPreferredName(), initialAnomalyScore).add(ANOMALY_SCORE.getPreferredName(), anomalyScore)
        .add(RAW_ANOMALY_SCORE.getPreferredName(), rawAnomalyScore);
    }
}
