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
import org.elasticsearch.xpack.ml.job.results.BucketInfluencer;

import java.io.IOException;
import java.util.Objects;


class BucketInfluencerNormalizable extends AbstractLeafNormalizable {
    private final BucketInfluencer bucketInfluencer;

    BucketInfluencerNormalizable(BucketInfluencer influencer, String indexName) {
        super(indexName);
        bucketInfluencer = Objects.requireNonNull(influencer);
    }

    @Override
    public String getId() {
        return bucketInfluencer.getId();
    }

    @Override
    public Level getLevel() {
        return BucketInfluencer.BUCKET_TIME.equals(bucketInfluencer.getInfluencerFieldName()) ?
                Level.ROOT : Level.BUCKET_INFLUENCER;
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
        return bucketInfluencer.getInfluencerFieldName();
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
        return bucketInfluencer.getProbability();
    }

    @Override
    public double getNormalizedScore() {
        return bucketInfluencer.getAnomalyScore();
    }

    @Override
    public void setNormalizedScore(double normalizedScore) {
        bucketInfluencer.setAnomalyScore(normalizedScore);
    }

    @Override
    public void setParentScore(double parentScore) {
        // Do nothing as it is not holding the parent score.
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return bucketInfluencer.toXContent(builder, params);
    }
}
