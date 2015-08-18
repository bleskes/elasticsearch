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

public class Influencer
{
    /**
     * Elasticsearch type
     */
    public static final String TYPE = "influencer";

    /*
     * Field names
     */
    public static final String PROBABILITY = "probability";
    public static final String TIMESTAMP = "timestamp";
    public static final String INFLUENCER_FIELD_NAME = "influencerFieldName";
    public static final String INFLUENCER_VALUE_NAME = "influencerFieldValue";
    public static final String INITIAL_SCORE = "initialScore";

    private double m_Probability;
    private Date m_Timestamp;

    private String m_InfluenceField;
    private String m_InfluenceValue;

    private double m_InitialScore;


    public Influencer()
    {
    }

    public Influencer(String fieldName, String fieldValue)
    {
        m_InfluenceField = fieldName;
        m_InfluenceValue = fieldValue;
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


    public double getInitialScore()
    {
        return m_InitialScore;
    }

    public void setInitialScore(double influenceScore)
    {
        this.m_InitialScore = influenceScore;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((m_Timestamp == null) ? 0 : m_Timestamp.hashCode());
        result = prime
                * result
                + ((m_InfluenceField == null) ? 0 : m_InfluenceField.hashCode());
        result = prime
                * result
                + ((m_InfluenceValue == null) ? 0 : m_InfluenceValue
                        .hashCode());

        long temp;
        temp = Double.doubleToLongBits(m_InitialScore);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(m_Probability);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
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
                Double.compare(m_InitialScore, other.m_InitialScore) == 0 &&
                Double.compare(m_Probability, other.m_Probability) == 0;
    }


}
