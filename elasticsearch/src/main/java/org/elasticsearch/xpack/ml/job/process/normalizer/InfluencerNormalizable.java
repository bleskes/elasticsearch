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
import org.elasticsearch.xpack.ml.job.results.Influencer;

import java.io.IOException;
import java.util.Objects;

class InfluencerNormalizable extends AbstractLeafNormalizable {
    private final Influencer influencer;

    InfluencerNormalizable(Influencer influencer, String indexName) {
        super(indexName);
        this.influencer = Objects.requireNonNull(influencer);
    }

    @Override
    public String getId() {
        return influencer.getId();
    }

    @Override
    public Level getLevel() {
        return Level.INFLUENCER;
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
        return influencer.getInfluencerFieldName();
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
        return influencer.getProbability();
    }

    @Override
    public double getNormalizedScore() {
        return influencer.getAnomalyScore();
    }

    @Override
    public void setNormalizedScore(double normalizedScore) {
        influencer.setAnomalyScore(normalizedScore);
    }

    @Override
    public void setParentScore(double parentScore) {
        throw new IllegalStateException("Influencer has no parent");
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return influencer.toXContent(builder, params);
    }
}
