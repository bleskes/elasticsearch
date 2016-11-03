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

import org.elasticsearch.xpack.prelert.job.results.AnomalyRecord;
import org.elasticsearch.xpack.prelert.job.results.Bucket;

import java.net.URI;
import java.util.Date;
import java.util.List;

/**
 * Encapsulate an Engine API alert. Alerts have:
 * <ol>
 * <li>Job Id - The source of the alert</li>
 * <li>Timestamp - The time of the alert </li>
 * <li>Bucket - The bucket that caused the alert if the alert was based on
 * anomaly score</li>
 * <li>Records - The records that caused the alert if the alert was based on a
 * normalized probability threshold</li>
 * <li>Alert Type see {@linkplain AlertType} the default is {@linkplain AlertType#BUCKET}
 * </ol>
 */
public class Alert {
    public static final String TYPE = "alert";

    public static final String JOB_ID = "JobId";
    public static final String TIMESTAMP = "timestamp";
    public static final String URI = "uri";


    private String jobId;
    private Date timestamp;
    private URI uri;
    private double anomalyScore;
    private double maxNormalizedProb;
    private boolean isTimeout;
    private AlertType alertType;
    private Bucket bucket;
    private List<AnomalyRecord> records;
    private boolean isInterim;

    public Alert() {
        alertType = AlertType.BUCKET;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public double getAnomalyScore() {
        return anomalyScore;
    }

    public void setAnomalyScore(double anomalyScore) {
        this.anomalyScore = anomalyScore;
    }

    public double getMaxNormalizedProbability() {
        return maxNormalizedProb;
    }

    public void setMaxNormalizedProbability(double prob) {
        this.maxNormalizedProb = prob;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public boolean isTimeout() {
        return isTimeout;
    }

    public void setTimeout(boolean timeout) {
        this.isTimeout = timeout;
    }

    public Bucket getBucket() {
        return bucket;
    }

    public void setBucket(Bucket bucket) {
        this.bucket = bucket;
    }

    public List<AnomalyRecord> getRecords() {
        return records;
    }

    public void setRecords(List<AnomalyRecord> records) {
        this.records = records;
    }

    public AlertType getAlertType() {
        return alertType;
    }

    public void setAlertType(AlertType value) {
        this.alertType = value;
    }

    public boolean isInterim() {
        return isInterim;
    }

    public void setInterim(boolean value) {
        this.isInterim = value;
    }

}
