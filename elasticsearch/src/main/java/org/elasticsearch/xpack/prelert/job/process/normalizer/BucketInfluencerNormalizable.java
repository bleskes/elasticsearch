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
    public void resetBigChangeFlag() {
        // Do nothing
    }

    @Override
    public void raiseBigChangeFlag() {
        // Do nothing
    }
}
