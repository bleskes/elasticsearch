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

import java.util.Date;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(value={"id", "initialAnomalyScore"}, allowSetters=true)
public class Influencer
{
    /**
     * Elasticsearch type
     */
    public static final String TYPE = "influencer";

    /*
     * Field names
     */
    public static final String ID = "id";
    public static final String PROBABILITY = "probability";
    public static final String TIMESTAMP = "timestamp";
    public static final String INFLUENCER_FIELD_NAME = "influencerFieldName";
    public static final String INFLUENCER_VALUE_NAME = "influencerFieldValue";
    public static final String INITIAL_ANOMALY_SCORE = "initialAnomalyScore";
    public static final String ANOMALY_SCORE = "anomalyScore";

    private Date m_Timestamp;
    private String m_InfluenceField;
    private String m_InfluenceValue;
    private double m_Probability;
    private double m_InitialAnomalyScore;
    private double m_AnomalyScore;
    private boolean m_HadBigNormalisedUpdate;

    public Influencer()
    {
    }

    public Influencer(String fieldName, String fieldValue)
    {
        m_InfluenceField = fieldName;
        m_InfluenceValue = fieldValue;
    }

    public String getId()
    {
        return new StringBuilder(m_InfluenceField)
                .append('_')
                .append(m_InfluenceValue)
                .append('_')
                .append(m_Timestamp.getTime() / 1000)
                .toString();
    }

    public double getProbability()
    {
        return m_Probability;
    }

    public void setProbability(double probability)
    {
        this.m_Probability = probability;
    }


    public Date getTimestamp()
    {
        return m_Timestamp;
    }

    public void setTimestamp(Date date)
    {
        this.m_Timestamp = date;
    }


    public String getInfluencerFieldName()
    {
        return m_InfluenceField;
    }

    public void setInfluencerFieldName(String fieldName)
    {
        this.m_InfluenceField = fieldName;
    }


    public String getInfluencerFieldValue()
    {
        return m_InfluenceValue;
    }

    public void setInfluencerFieldValue(String fieldValue)
    {
        this.m_InfluenceValue = fieldValue;
    }

    public double getInitialAnomalyScore()
    {
        return m_InitialAnomalyScore;
    }

    public void setInitialAnomalyScore(double influenceScore)
    {
        this.m_InitialAnomalyScore = influenceScore;
    }

    public double getAnomalyScore()
    {
        return m_AnomalyScore;
    }

    public void setAnomalyScore(double score)
    {
        m_AnomalyScore = score;
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

    @Override
    public int hashCode()
    {
        return Objects.hash(m_Timestamp, m_InfluenceField, m_InfluenceValue, m_InitialAnomalyScore,
                m_AnomalyScore, m_Probability);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (obj == null)
        {
            return false;
        }

        if (getClass() != obj.getClass())
        {
            return false;
        }

        Influencer other = (Influencer) obj;

        return Objects.equals(m_Timestamp, other.m_Timestamp) &&
                Objects.equals(m_InfluenceField, other.m_InfluenceField) &&
                Objects.equals(m_InfluenceValue, other.m_InfluenceValue) &&
                Double.compare(m_InitialAnomalyScore, other.m_InitialAnomalyScore) == 0 &&
                Double.compare(m_AnomalyScore, other.m_AnomalyScore) == 0 &&
                Double.compare(m_Probability, other.m_Probability) == 0;
    }


}
