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
package org.elasticsearch.xpack.ml.job.results;

import org.elasticsearch.action.support.ToXContentToBytes;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParseFieldMatcherSupplier;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.XContentBuilder;
import java.io.IOException;
import java.util.Objects;

public class PartitionScore extends ToXContentToBytes implements Writeable {
    public static final ParseField PARTITION_SCORE = new ParseField("partition_score");

    private final String partitionFieldValue;
    private final String partitionFieldName;
    private final double initialAnomalyScore;
    private double anomalyScore;
    private double probability;
    private boolean hadBigNormalizedUpdate;

    public static final ConstructingObjectParser<PartitionScore, ParseFieldMatcherSupplier> PARSER = new ConstructingObjectParser<>(
            PARTITION_SCORE.getPreferredName(), a -> new PartitionScore((String) a[0], (String) a[1], (Double) a[2], (Double) a[3],
            (Double) a[4]));

    static {
        PARSER.declareString(ConstructingObjectParser.constructorArg(), AnomalyRecord.PARTITION_FIELD_NAME);
        PARSER.declareString(ConstructingObjectParser.constructorArg(), AnomalyRecord.PARTITION_FIELD_VALUE);
        PARSER.declareDouble(ConstructingObjectParser.constructorArg(), Bucket.INITIAL_ANOMALY_SCORE);
        PARSER.declareDouble(ConstructingObjectParser.constructorArg(), AnomalyRecord.ANOMALY_SCORE);
        PARSER.declareDouble(ConstructingObjectParser.constructorArg(), AnomalyRecord.PROBABILITY);
    }

    public PartitionScore(String fieldName, String fieldValue, double initialAnomalyScore, double anomalyScore, double probability) {
        hadBigNormalizedUpdate = false;
        partitionFieldName = fieldName;
        partitionFieldValue = fieldValue;
        this.initialAnomalyScore = initialAnomalyScore;
        this.anomalyScore = anomalyScore;
        this.probability = probability;
    }

    public PartitionScore(StreamInput in) throws IOException {
        partitionFieldName = in.readString();
        partitionFieldValue = in.readString();
        initialAnomalyScore = in.readDouble();
        anomalyScore = in.readDouble();
        probability = in.readDouble();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(partitionFieldName);
        out.writeString(partitionFieldValue);
        out.writeDouble(initialAnomalyScore);
        out.writeDouble(anomalyScore);
        out.writeDouble(probability);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(AnomalyRecord.PARTITION_FIELD_NAME.getPreferredName(), partitionFieldName);
        builder.field(AnomalyRecord.PARTITION_FIELD_VALUE.getPreferredName(), partitionFieldValue);
        builder.field(Bucket.INITIAL_ANOMALY_SCORE.getPreferredName(), initialAnomalyScore);
        builder.field(AnomalyRecord.ANOMALY_SCORE.getPreferredName(), anomalyScore);
        builder.field(AnomalyRecord.PROBABILITY.getPreferredName(), probability);
        builder.endObject();
        return builder;
    }

    public double getInitialAnomalyScore() {
        return initialAnomalyScore;
    }

    public double getAnomalyScore() {
        return anomalyScore;
    }

    public void setAnomalyScore(double anomalyScore) {
        this.anomalyScore = anomalyScore;
    }

    public String getPartitionFieldName() {
        return partitionFieldName;
    }

    public String getPartitionFieldValue() {
        return partitionFieldValue;
    }

    public double getProbability() {
        return probability;
    }

    public void setProbability(double probability) {
        this.probability = probability;
    }

    @Override
    public int hashCode() {
        return Objects.hash(partitionFieldName, partitionFieldValue, probability, initialAnomalyScore, anomalyScore);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other instanceof PartitionScore == false) {
            return false;
        }

        PartitionScore that = (PartitionScore) other;

        // hadBigNormalizedUpdate is deliberately excluded from the test
        // as is id, which is generated by the datastore
        return Objects.equals(this.partitionFieldValue, that.partitionFieldValue)
                && Objects.equals(this.partitionFieldName, that.partitionFieldName) && (this.probability == that.probability)
                && (this.initialAnomalyScore == that.initialAnomalyScore) && (this.anomalyScore == that.anomalyScore);
    }

    public boolean hadBigNormalizedUpdate() {
        return hadBigNormalizedUpdate;
    }

    public void resetBigNormalizedUpdateFlag() {
        hadBigNormalizedUpdate = false;
    }

    public void raiseBigNormalizedUpdateFlag() {
        hadBigNormalizedUpdate = true;
    }
}
