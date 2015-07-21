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
    public static final String BUCKET_ID = "bucketId";
    public static final String FIELD_NAME = "fieldName";
    public static final String FIELD_VALUE = "fieldValue";
    public static final String INFLUENCE_SCORE = "influenceScore";

    private double m_Probability;
    private String m_BucketId;

    private String m_FieldName;
    private String m_FieldValue;

    private double m_InfluenceScore;


    public Influencer()
    {
    }

    public Influencer(String fieldName, String fieldValue)
    {
        m_FieldName = fieldName;
        m_FieldValue = fieldValue;
    }

    public double getProbability()
    {
        return m_Probability;
    }

    public void setProbability(double probability)
    {
        this.m_Probability = probability;
    }


    public String getBucketId()
    {
        return m_BucketId;
    }

    public void setBucketId(String bucketId)
    {
        this.m_BucketId = bucketId;
    }


    public String getFieldName()
    {
        return m_FieldName;
    }

    public void setFieldName(String fieldName)
    {
        this.m_FieldName = fieldName;
    }


    public String getFieldValue()
    {
        return m_FieldValue;
    }

    public void setFieldValue(String fieldValue)
    {
        this.m_FieldValue = fieldValue;
    }


    public double getInfluenceScore()
    {
        return m_InfluenceScore;
    }

    public void setInfluenceScore(double influenceScore)
    {
        this.m_InfluenceScore = influenceScore;
    }
}
