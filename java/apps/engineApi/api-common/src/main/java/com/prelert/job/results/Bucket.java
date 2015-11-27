/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/

package com.prelert.job.results;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Bucket Result POJO
 */
@JsonIgnoreProperties({"epoch", "detectors", "normalisable"})
@JsonInclude(Include.NON_NULL)
public class Bucket
{
    /*
     * Field Names
     */
    public static final String ID = "id";
    public static final String TIMESTAMP = "timestamp";
    public static final String ANOMALY_SCORE = "anomalyScore";
    public static final String MAX_NORMALIZED_PROBABILITY = "maxNormalizedProbability";
    public static final String IS_INTERIM = "isInterim";
    public static final String RECORD_COUNT = "recordCount";
    public static final String EVENT_COUNT = "eventCount";
    public static final String DETECTORS = "detectors";
    public static final String RECORDS = "records";
    public static final String BUCKET_INFLUENCERS = "bucketInfluencers";
    public static final String INFLUENCERS = "influencers";

    public static final String ES_TIMESTAMP = "@timestamp";

    /**
     * This is a debug only field. It is only written in ES; the Java objects
     * never get these values.
     */
    public static final String INITIAL_ANOMALY_SCORE = "initialAnomalyScore";


    /**
     * Elasticsearch type
     */
    public static final String TYPE = "bucket";

    private static final Logger LOGGER = Logger.getLogger(Bucket.class);

    private Date m_Timestamp;
    private double m_AnomalyScore;
    private double m_MaxNormalizedProbability;
    private int m_RecordCount;
    private List<Detector> m_Detectors;
    private List<AnomalyRecord> m_Records;
    private long m_EventCount;
    private Boolean m_IsInterim;
    private boolean m_HadBigNormalisedUpdate;
    private List<BucketInfluencer> m_BucketInfluencers;
    private List<Influencer> m_Influencers;

    public Bucket()
    {
        m_Detectors = new ArrayList<>();
        m_Records = Collections.emptyList();
        m_BucketInfluencers = new ArrayList<>();
    }

    /**
     * The bucket Id is the bucket's timestamp in seconds
     * from the epoch. As the id is derived from the timestamp
     * field it doesn't need to be serialised, however, in the
     * past it was serialised accidentally, so it still is.
     *
     * @return The bucket id
     */
    public String getId()
    {
        return Long.toString(getEpoch()).intern();
    }

    /**
     * Set the ID and derive the timestamp from it.  It MUST be
     * a number that corresponds to the bucket's timestamp in seconds
     * from the epoch.
     */
    public void setId(String id)
    {
        try
        {
            long epoch = Long.parseLong(id);
            m_Timestamp = new Date(epoch * 1000);
        }
        catch (NumberFormatException nfe)
        {
            LOGGER.error("Could not parse ID " + id + " as a long");
        }
    }

    /**
     * Timestamp expressed in seconds since the epoch (rather than Java's
     * convention of milliseconds).
     */
    public long getEpoch()
    {
        return m_Timestamp.getTime() / 1000;
    }

    public Date getTimestamp()
    {
        return m_Timestamp;
    }

    public void setTimestamp(Date timestamp)
    {
        m_Timestamp = timestamp;
    }

    public double getAnomalyScore()
    {
        return m_AnomalyScore;
    }

    public void setAnomalyScore(double anomalyScore)
    {
        m_AnomalyScore = anomalyScore;
    }

    public double getMaxNormalizedProbability()
    {
        return m_MaxNormalizedProbability;
    }

    public void setMaxNormalizedProbability(double maxNormalizedProbability)
    {
        m_MaxNormalizedProbability = maxNormalizedProbability;
    }

    public int getRecordCount()
    {
        return m_RecordCount;
    }

    public void setRecordCount(int recordCount)
    {
        m_RecordCount = recordCount;
    }


    /**
     * Get the list of detectors that produced output in this bucket
     *
     * @return A list of detector
     */
    public List<Detector> getDetectors()
    {
        return m_Detectors;
    }

    public void setDetectors(List<Detector> detectors)
    {
        m_Detectors = detectors;
    }


    /**
     * Add a detector that produced output in this bucket
     *
     */
    public void addDetector(Detector detector)
    {
        m_Detectors.add(detector);
    }


    /**
     * Get all the anomaly records associated with this bucket
     * @return All the anomaly records
     */
    public List<AnomalyRecord> getRecords()
    {
        return m_Records;
    }

    public void setRecords(List<AnomalyRecord> records)
    {
        m_Records = records;
    }

    /**
     * The number of records (events) actually processed
     * in this bucket.
     * @return
     */
    public long getEventCount()
    {
        return m_EventCount;
    }

    public void setEventCount(long value)
    {
        m_EventCount = value;
    }

    @JsonProperty("isInterim")
    public Boolean isInterim()
    {
        return m_IsInterim;
    }

    @JsonProperty("isInterim")
    public void setInterim(Boolean isInterim)
    {
        m_IsInterim = isInterim;
    }

    public List<Influencer> getInfluencers()
    {
        return m_Influencers;
    }

    public void setInfluencers(List<Influencer> influences)
    {
        this.m_Influencers = influences;
    }

    public List<BucketInfluencer> getBucketInfluencers()
    {
        return m_BucketInfluencers;
    }

    public void setBucketInfluencers(List<BucketInfluencer> bucketInfluencers)
    {
        m_BucketInfluencers = bucketInfluencers;
    }

    public void addBucketInfluencer(BucketInfluencer bucketInfluencer)
    {
        if (m_BucketInfluencers == null)
        {
            m_BucketInfluencers = new ArrayList<>();
        }
        m_BucketInfluencers.add(bucketInfluencer);
    }

    @Override
    public int hashCode()
    {
        // m_HadBigNormalisedUpdate is deliberately excluded from the hash
        return Objects.hash(m_Timestamp, m_EventCount, m_AnomalyScore, m_MaxNormalizedProbability,
                m_RecordCount, m_Records, m_IsInterim, m_BucketInfluencers, m_Influencers);
    }

    /**
     * Compare all the fields and embedded anomaly records
     * (if any), does not compare detectors as they are not
     * serialized anyway.
     */
    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (other instanceof Bucket == false)
        {
            return false;
        }

        Bucket that = (Bucket)other;

        // m_HadBigNormalisedUpdate is deliberately excluded from the test
        // don't bother testing detectors
        return Objects.equals(this.m_Timestamp, that.m_Timestamp)
                && (this.m_EventCount == that.m_EventCount)
                && (this.m_AnomalyScore == that.m_AnomalyScore)
                && (this.m_MaxNormalizedProbability == that.m_MaxNormalizedProbability)
                && (this.m_RecordCount == that.m_RecordCount)
                && Objects.equals(this.m_Records, that.m_Records)
                && Objects.equals(this.m_IsInterim, that.m_IsInterim)
                && Objects.equals(this.m_BucketInfluencers, that.m_BucketInfluencers)
                && Objects.equals(this.m_Influencers, that.m_Influencers);
    }

    public boolean hadBigNormalisedUpdate()
    {
        return m_HadBigNormalisedUpdate;
    }

    public void resetBigNormalisedUpdateFlag()
    {
        m_HadBigNormalisedUpdate = false;
    }

    public void raiseBigNormalisedUpdateFlag()
    {
        m_HadBigNormalisedUpdate = true;
    }

    /**
     * This method encapsulated the logic for whether a bucket should
     * be normalised. The decision depends on two factors.
     *
     * The first is whether the bucket has bucket influencers.
     * Since bucket influencers were introduced, every bucket must have
     * at least one bucket influencer. If it does not, it means it is
     * a bucket persisted with an older version and should not be
     * normalised.
     *
     * The second factor has to do with minimising the number of buckets
     * that are sent for normalisation. Buckets that have no records
     * and a score of zero should not be normalised as their score
     * will not change and they will just add overhead.
     *
     * @return true if the bucket should be normalised or false otherwise
     */
    public boolean isNormalisable()
    {
        if (m_BucketInfluencers == null || m_BucketInfluencers.isEmpty())
        {
            return false;
        }
        return m_AnomalyScore > 0.0 || m_RecordCount > 0;
    }
}
