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
package org.elasticsearch.xpack.prelert.job.alert;


import org.elasticsearch.xpack.prelert.job.results.Bucket;
import org.elasticsearch.xpack.prelert.job.results.BucketInfluencer;
import org.elasticsearch.xpack.prelert.job.results.Influencer;

/**
 * Simple immutable class to encapsulate the alerting options
 */
public class AlertTrigger {
    /**
     * If null it means it was not specified.
     */
    private final Double anomalyThreshold;

    /**
     * If null it means it was not specified.
     */
    private final Double normalisedThreshold;

    private final AlertType alertType;

    private final boolean includeInterim;

    public AlertTrigger(Double normlizedProbThreshold, Double anomalyThreshold, AlertType type) {
        this(normlizedProbThreshold, anomalyThreshold, type, false);
    }

    public AlertTrigger(Double normlizedProbThreshold, Double anomalyThreshold,
            AlertType type, boolean includeInterim) {
        normalisedThreshold = normlizedProbThreshold;
        this.anomalyThreshold = anomalyThreshold;
        alertType = type;
        this.includeInterim = includeInterim;
    }

    public Double getNormalisedThreshold() {
        return normalisedThreshold;
    }

    public Double getAnomalyThreshold() {
        return anomalyThreshold;
    }

    public AlertType getAlertType() {
        return alertType;
    }

    public boolean isIncludeInterim() {
        return includeInterim;
    }

    boolean isTriggeredBy(Bucket bucket) {
        if (bucket.isInterim() && !includeInterim) {
            return false;
        }

        switch (alertType) {
        case BUCKET:
            return isTriggeredByBucket(bucket.getMaxNormalizedProbability(), bucket.getAnomalyScore());
        case BUCKETINFLUENCER:
            return isTriggeredByBucketInfluencers(bucket);
        case INFLUENCER:
            return isTriggeredByInfluencers(bucket);
        default:
            return false;
        }
    }

    private boolean isTriggeredByBucket(double normalisedValue, double anomalyScore) {
        return isGreaterOrEqual(normalisedValue, normalisedThreshold)
                || isGreaterOrEqual(anomalyScore, anomalyThreshold);
    }

    private static boolean isGreaterOrEqual(double value, Double threshold) {
        return threshold == null ? false : value >= threshold;
    }

    private boolean isTriggeredByBucketInfluencers(Bucket bucket) {
        for (BucketInfluencer bi : bucket.getBucketInfluencers()) {
            if (isGreaterOrEqual(bi.getAnomalyScore(), anomalyThreshold)) {
                return true;
            }
        }
        return false;
    }

    private boolean isTriggeredByInfluencers(Bucket bucket) {
        for (Influencer inf : bucket.getInfluencers()) {
            if (isGreaterOrEqual(inf.getAnomalyScore(), anomalyThreshold)) {
                return true;
            }
        }
        return false;
    }

    boolean triggersAnomalyThreshold(double value) {
        return isGreaterOrEqual(value, anomalyThreshold);
    }
}
