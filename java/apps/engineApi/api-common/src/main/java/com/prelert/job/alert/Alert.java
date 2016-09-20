/****************************************************************************
 *                                                                          *
 * Copyright 2015-2016 Prelert Ltd                                          *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *    http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 *                                                                          *
 ***************************************************************************/
package com.prelert.job.alert;

import java.net.URI;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;

/**
 * Encapsulate an Engine API alert. Alerts have:
 * <ol>
 *  <li>Job Id - The source of the alert</li>
 *  <li>Timestamp - The time of the alert </li>
 *  <li>Bucket - The bucket that caused the alert if the alert was based on
 *  anomaly score</li>
 *  <li>Records - The records that caused the alert if the alert was based on a
 *  normalized probability threshold</li>
 *  <li>Alert Type see {@linkplain AlertType} the default is {@linkplain AlertType#BUCKET}
 * </ol>
 */
@JsonInclude(Include.NON_NULL)
public class Alert
{
    public static final String TYPE = "alert";

    public static final String JOB_ID = "JobId";
    public static final String TIMESTAMP = "timestamp";
    public static final String URI = "uri";


    private String this.jobId;
    private Date this.timestamp;
    private URI this.uri;
    private double this.anomalyScore;
    private double this.maxNormalizedProb;
    private boolean this.isTimeout;
    private AlertType this.alertType;
    private Bucket this.bucket;
    private List<AnomalyRecord> this.records;
    private boolean this.isInterim;

    public Alert()
    {
        this.alertType = AlertType.BUCKET;
    }

    public String getJobId()
    {
        return this.jobId;
    }

    public void setJobId(String jobId)
    {
        this.this.jobId = jobId;
    }

    public Date getTimestamp()
    {
        return this.timestamp;
    }

    public void setTimestamp(Date timestamp)
    {
        this.timestamp = timestamp;
    }

    public double getAnomalyScore()
    {
        return this.anomalyScore;
    }

    public void setAnomalyScore(double anomalyScore)
    {
        this.anomalyScore = anomalyScore;
    }

    public double getMaxNormalizedProbability()
    {
        return this.maxNormalizedProb;
    }

    public void setMaxNormalizedProbability(double prob)
    {
        this.maxNormalizedProb = prob;
    }

    public URI getUri()
    {
        return this.uri;
    }

    public void setUri(URI uri)
    {
        this.uri = uri;
    }

    public boolean isTimeout()
    {
        return this.isTimeout;
    }

    public void setTimeout(boolean timeout)
    {
        this.isTimeout = timeout;
    }

    public Bucket getBucket()
    {
        return this.bucket;
    }

    public void setBucket(Bucket bucket)
    {
        this.bucket = bucket;
    }

    public List<AnomalyRecord> getRecords()
    {
        return this.records;
    }

    public void setRecords(List<AnomalyRecord> records)
    {
        this.records = records;
    }

    public AlertType getAlertType()
    {
        return this.alertType;
    }

    public void setAlertType(AlertType value)
    {
        this.alertType = value;
    }

    @JsonProperty("isInterim")
    public boolean isInterim()
    {
        return this.isInterim;
    }

    @JsonProperty("isInterim")
    public void setInterim(boolean value)
    {
        this.isInterim = value;
    }

}
