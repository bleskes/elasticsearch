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

import org.elasticsearch.xpack.ml.job.results.PartitionScore;

import java.util.Objects;


public class PartitionScoreNormalizable extends AbstractLeafNormalizable {
    private final PartitionScore score;

    public PartitionScoreNormalizable(PartitionScore score) {
        this.score = Objects.requireNonNull(score);
    }

    @Override
    public Level getLevel() {
        return Level.PARTITION;
    }

    @Override
    public String getPartitionFieldName() {
        return score.getPartitionFieldName();
    }

    @Override
    public String getPartitionFieldValue() {
        return score.getPartitionFieldValue();
    }

    @Override
    public String getPersonFieldName() {
        return null;
    }

    @Override
    public String getFunctionName() {
        return null;
    }

    @Override
    public String getValueFieldName() {
        return null;
    }

    @Override
    public double getProbability() {
        return score.getProbability();
    }

    @Override
    public double getNormalizedScore() {
        return score.getAnomalyScore();
    }

    @Override
    public void setNormalizedScore(double normalizedScore) {
        score.setAnomalyScore(normalizedScore);
    }

    @Override
    public void setParentScore(double parentScore) {
        // Do nothing as it is not holding the parent score.
    }

    @Override
    public void resetBigChangeFlag() {
        score.resetBigNormalizedUpdateFlag();
    }

    @Override
    public void raiseBigChangeFlag() {
        score.raiseBigNormalizedUpdateFlag();
    }
}
