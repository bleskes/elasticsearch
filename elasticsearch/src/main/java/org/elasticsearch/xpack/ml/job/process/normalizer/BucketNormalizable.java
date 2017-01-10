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

import org.elasticsearch.xpack.ml.job.results.Bucket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


class BucketNormalizable implements Normalizable {
    private static final int BUCKET_INFLUENCER = 0;
    private static final int RECORD = 1;
    private static final int PARTITION_SCORE = 2;
    private static final List<Integer> CHILDREN_TYPES =
            Arrays.asList(BUCKET_INFLUENCER, RECORD, PARTITION_SCORE);

    private final Bucket bucket;

    public BucketNormalizable(Bucket bucket) {
        this.bucket = Objects.requireNonNull(bucket);
    }

    @Override
    public boolean isContainerOnly() {
        return true;
    }

    @Override
    public Level getLevel() {
        return Level.ROOT;
    }

    @Override
    public String getPartitionFieldName() {
        return null;
    }

    @Override
    public String getPartitionFieldValue() {
        return null;
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
        throw new IllegalStateException("Bucket is container only");
    }

    @Override
    public double getNormalizedScore() {
        return bucket.getAnomalyScore();
    }

    @Override
    public void setNormalizedScore(double normalizedScore) {
        bucket.setAnomalyScore(normalizedScore);
    }

    @Override
    public List<Integer> getChildrenTypes() {
        return CHILDREN_TYPES;
    }

    @Override
    public List<Normalizable> getChildren() {
        List<Normalizable> children = new ArrayList<>();
        for (Integer type : getChildrenTypes()) {
            children.addAll(getChildren(type));
        }
        return children;
    }

    @Override
    public List<Normalizable> getChildren(int type) {
        List<Normalizable> children = new ArrayList<>();
        switch (type) {
            case BUCKET_INFLUENCER:
                bucket.getBucketInfluencers().stream().forEach(
                        influencer -> children.add(new BucketInfluencerNormalizable(influencer)));
                break;
            case RECORD:
                bucket.getRecords().stream().forEach(
                        record -> children.add(new RecordNormalizable(record)));
                break;
            case PARTITION_SCORE:
                bucket.getPartitionScores().stream().forEach(
                        partitionScore -> children.add(new PartitionScoreNormalizable(partitionScore)));
                break;
            default:
                throw new IllegalArgumentException("Invalid type: " + type);
        }
        return children;
    }

    @Override
    public boolean setMaxChildrenScore(int childrenType, double maxScore) {
        double oldScore = 0.0;
        switch (childrenType) {
            case BUCKET_INFLUENCER:
                oldScore = bucket.getAnomalyScore();
                bucket.setAnomalyScore(maxScore);
                break;
            case RECORD:
                oldScore = bucket.getMaxNormalizedProbability();
                bucket.setMaxNormalizedProbability(maxScore);
                break;
            case PARTITION_SCORE:
                break;
            default:
                throw new IllegalArgumentException("Invalid type: " + childrenType);
        }
        return maxScore != oldScore;
    }

    @Override
    public void setParentScore(double parentScore) {
        throw new IllegalStateException("Bucket has no parent");
    }

    @Override
    public void resetBigChangeFlag() {
        bucket.resetBigNormalizedUpdateFlag();
    }

    @Override
    public void raiseBigChangeFlag() {
        bucket.raiseBigNormalizedUpdateFlag();
    }
}
