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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import org.elasticsearch.action.support.ToXContentToBytes;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParseFieldMatcherSupplier;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ObjectParser;
import org.elasticsearch.common.xcontent.ObjectParser.ValueType;
import org.elasticsearch.common.xcontent.XContentParser.Token;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.utils.time.TimeUtils;

import java.io.IOException;
import java.util.Date;
import java.util.Objects;

@JsonInclude(Include.NON_NULL)
public class BucketInfluencer extends ToXContentToBytes implements Writeable {
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
    public static final ParseField IS_INTERIM = new ParseField("isInterim");
    public static final ParseField TIMESTAMP = new ParseField("timestamp");

    public static final ObjectParser<BucketInfluencer, ParseFieldMatcherSupplier> PARSER = new ObjectParser<>(TYPE.getPreferredName(),
            BucketInfluencer::new);

    static {
        PARSER.declareString(BucketInfluencer::setInfluencerFieldName, INFLUENCER_FIELD_NAME);
        PARSER.declareDouble(BucketInfluencer::setInitialAnomalyScore, INITIAL_ANOMALY_SCORE);
        PARSER.declareDouble(BucketInfluencer::setAnomalyScore, ANOMALY_SCORE);
        PARSER.declareDouble(BucketInfluencer::setRawAnomalyScore, RAW_ANOMALY_SCORE);
        PARSER.declareDouble(BucketInfluencer::setProbability, PROBABILITY);
        PARSER.declareBoolean(BucketInfluencer::setIsInterim, IS_INTERIM);
        PARSER.declareField(BucketInfluencer::setTimestamp, p -> {
            if (p.currentToken() == Token.VALUE_NUMBER) {
                return new Date(p.longValue());
            } else if (p.currentToken() == Token.VALUE_STRING) {
                return new Date(TimeUtils.dateStringToEpoch(p.text()));
            }
            throw new IllegalArgumentException("unexpected token [" + p.currentToken() + "] for [" + TIMESTAMP.getPreferredName() + "]");
        }, TIMESTAMP, ValueType.VALUE);
        PARSER.declareString(BucketInfluencer::setJobId, JobDetails.ID);
    }

    private String influenceField;
    private double initialAnomalyScore;
    private double anomalyScore;
    private double rawAnomalyScore;
    private double probability;
    private boolean isInterim;
    private String jobId;
    private Date timestamp;

    public BucketInfluencer() {
        // Default constructor
    }

    public BucketInfluencer(BucketInfluencer prototype) {
        influenceField = prototype.influenceField;
        initialAnomalyScore = prototype.initialAnomalyScore;
        anomalyScore = prototype.anomalyScore;
        rawAnomalyScore = prototype.rawAnomalyScore;
        probability = prototype.probability;
        isInterim = prototype.isInterim;
        jobId = prototype.jobId;
        timestamp = prototype.timestamp;
    }

    public BucketInfluencer(StreamInput in) throws IOException {
        influenceField = in.readOptionalString();
        initialAnomalyScore = in.readDouble();
        anomalyScore = in.readDouble();
        rawAnomalyScore = in.readDouble();
        probability = in.readDouble();
        isInterim = in.readBoolean();
        jobId = in.readOptionalString();
        if (in.readBoolean()) {
            timestamp = new Date(in.readLong());
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeOptionalString(influenceField);
        out.writeDouble(initialAnomalyScore);
        out.writeDouble(anomalyScore);
        out.writeDouble(rawAnomalyScore);
        out.writeDouble(probability);
        out.writeBoolean(isInterim);
        out.writeOptionalString(jobId);
        boolean hasTimestamp = timestamp != null;
        out.writeBoolean(hasTimestamp);
        if (hasTimestamp) {
            out.writeLong(timestamp.getTime());
        }
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
        if (jobId != null) {
            builder.field(JobDetails.ID.getPreferredName(), jobId);
        }
        if (timestamp != null) {
            builder.field(TIMESTAMP.getPreferredName(), timestamp.getTime());
        }
        builder.field(IS_INTERIM.getPreferredName(), isInterim);
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

    @JsonProperty("isInterim")
    public void setIsInterim(boolean isInterim) {
        this.isInterim = isInterim;
    }

    @JsonProperty("isInterim")
    public boolean isInterim() {
        return isInterim;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getJobId() {
        return jobId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(influenceField, initialAnomalyScore, anomalyScore, rawAnomalyScore, probability, isInterim, timestamp, jobId);
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
                && Double.compare(probability, other.probability) == 0 && Objects.equals(isInterim, other.isInterim)
                && Objects.equals(timestamp, other.timestamp) && Objects.equals(jobId, other.jobId);
    }
}
