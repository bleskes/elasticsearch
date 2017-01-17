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

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.xpack.ml.job.results.AnomalyRecord;

import java.io.IOException;
import java.util.Objects;


class RecordNormalizable extends AbstractLeafNormalizable {
    private final AnomalyRecord record;

    public RecordNormalizable(AnomalyRecord record, String indexName) {
        super(indexName);
        this.record = Objects.requireNonNull(record);
    }

    @Override
    public String getId() {
        return record.getId();
    }

    @Override
    public Level getLevel() {
        return Level.LEAF;
    }

    @Override
    public String getPartitionFieldName() {
        return record.getPartitionFieldName();
    }

    @Override
    public String getPartitionFieldValue() {
        return record.getPartitionFieldValue();
    }

    @Override
    public String getPersonFieldName() {
        String over = record.getOverFieldName();
        return over != null ? over : record.getByFieldName();
    }

    @Override
    public String getFunctionName() {
        return record.getFunction();
    }

    @Override
    public String getValueFieldName() {
        return record.getFieldName();
    }

    @Override
    public double getProbability() {
        return record.getProbability();
    }

    @Override
    public double getNormalizedScore() {
        return record.getNormalizedProbability();
    }

    @Override
    public void setNormalizedScore(double normalizedScore) {
        record.setNormalizedProbability(normalizedScore);
    }

    @Override
    public void setParentScore(double parentScore) {
        record.setAnomalyScore(parentScore);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return record.toXContent(builder, params);
    }

    public AnomalyRecord getRecord() {
        return record;
    }
}
