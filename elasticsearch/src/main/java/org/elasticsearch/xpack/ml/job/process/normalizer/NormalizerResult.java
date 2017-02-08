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
package org.elasticsearch.xpack.ml.job.process.normalizer;

import org.elasticsearch.action.support.ToXContentToBytes;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ObjectParser;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Objects;

/**
 * Parse the output of the normalizer process, for example:
 *
 * {"probability":0.01,"normalized_score":2.2}
 */
public class NormalizerResult extends ToXContentToBytes implements Writeable {
    static final ParseField LEVEL_FIELD = new ParseField("level");
    static final ParseField PARTITION_FIELD_NAME_FIELD = new ParseField("partition_field_name");
    static final ParseField PARTITION_FIELD_VALUE_FIELD = new ParseField("partition_field_value");
    static final ParseField PERSON_FIELD_NAME_FIELD = new ParseField("person_field_name");
    static final ParseField FUNCTION_NAME_FIELD = new ParseField("function_name");
    static final ParseField VALUE_FIELD_NAME_FIELD = new ParseField("value_field_name");
    static final ParseField PROBABILITY_FIELD = new ParseField("probability");
    static final ParseField NORMALIZED_SCORE_FIELD = new ParseField("normalized_score");

    public static final ObjectParser<NormalizerResult, Void> PARSER = new ObjectParser<>(
            LEVEL_FIELD.getPreferredName(), NormalizerResult::new);

    static {
        PARSER.declareString(NormalizerResult::setLevel, LEVEL_FIELD);
        PARSER.declareString(NormalizerResult::setPartitionFieldName, PARTITION_FIELD_NAME_FIELD);
        PARSER.declareString(NormalizerResult::setPartitionFieldValue, PARTITION_FIELD_VALUE_FIELD);
        PARSER.declareString(NormalizerResult::setPersonFieldName, PERSON_FIELD_NAME_FIELD);
        PARSER.declareString(NormalizerResult::setFunctionName, FUNCTION_NAME_FIELD);
        PARSER.declareString(NormalizerResult::setValueFieldName, VALUE_FIELD_NAME_FIELD);
        PARSER.declareDouble(NormalizerResult::setProbability, PROBABILITY_FIELD);
        PARSER.declareDouble(NormalizerResult::setNormalizedScore, NORMALIZED_SCORE_FIELD);
    }

    private String level;
    private String partitionFieldName;
    private String partitionFieldValue;
    private String personFieldName;
    private String functionName;
    private String valueFieldName;
    private double probability;
    private double normalizedScore;

    public NormalizerResult() {
    }

    public NormalizerResult(StreamInput in) throws IOException {
        level = in.readOptionalString();
        partitionFieldName = in.readOptionalString();
        partitionFieldValue = in.readOptionalString();
        personFieldName = in.readOptionalString();
        functionName = in.readOptionalString();
        valueFieldName = in.readOptionalString();
        probability = in.readDouble();
        normalizedScore = in.readDouble();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeOptionalString(level);
        out.writeOptionalString(partitionFieldName);
        out.writeOptionalString(partitionFieldValue);
        out.writeOptionalString(personFieldName);
        out.writeOptionalString(functionName);
        out.writeOptionalString(valueFieldName);
        out.writeDouble(probability);
        out.writeDouble(normalizedScore);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(LEVEL_FIELD.getPreferredName(), level);
        builder.field(PARTITION_FIELD_NAME_FIELD.getPreferredName(), partitionFieldName);
        builder.field(PARTITION_FIELD_VALUE_FIELD.getPreferredName(), partitionFieldValue);
        builder.field(PERSON_FIELD_NAME_FIELD.getPreferredName(), personFieldName);
        builder.field(FUNCTION_NAME_FIELD.getPreferredName(), functionName);
        builder.field(VALUE_FIELD_NAME_FIELD.getPreferredName(), valueFieldName);
        builder.field(PROBABILITY_FIELD.getPreferredName(), probability);
        builder.field(NORMALIZED_SCORE_FIELD.getPreferredName(), normalizedScore);
        builder.endObject();
        return builder;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getPartitionFieldName() {
        return partitionFieldName;
    }

    public void setPartitionFieldName(String partitionFieldName) {
        this.partitionFieldName = partitionFieldName;
    }

    public String getPartitionFieldValue() {
        return partitionFieldValue;
    }

    public void setPartitionFieldValue(String partitionFieldValue) {
        this.partitionFieldValue = partitionFieldValue;
    }

    public String getPersonFieldName() {
        return personFieldName;
    }

    public void setPersonFieldName(String personFieldName) {
        this.personFieldName = personFieldName;
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public String getValueFieldName() {
        return valueFieldName;
    }

    public void setValueFieldName(String valueFieldName) {
        this.valueFieldName = valueFieldName;
    }

    public double getProbability() {
        return probability;
    }

    public void setProbability(double probability) {
        this.probability = probability;
    }

    public double getNormalizedScore() {
        return normalizedScore;
    }

    public void setNormalizedScore(double normalizedScore) {
        this.normalizedScore = normalizedScore;
    }

    @Override
    public int hashCode() {
        return Objects.hash(level, partitionFieldName, partitionFieldValue, personFieldName,
                functionName, valueFieldName, probability, normalizedScore);
    }

    /**
     * Compare all the fields.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof NormalizerResult)) {
            return false;
        }

        NormalizerResult that = (NormalizerResult)other;

        return Objects.equals(this.level, that.level)
                && Objects.equals(this.partitionFieldName, that.partitionFieldName)
                && Objects.equals(this.partitionFieldValue, that.partitionFieldValue)
                && Objects.equals(this.personFieldName, that.personFieldName)
                && Objects.equals(this.functionName, that.functionName)
                && Objects.equals(this.valueFieldName, that.valueFieldName)
                && this.probability == that.probability
                && this.normalizedScore == that.normalizedScore;
    }
}
