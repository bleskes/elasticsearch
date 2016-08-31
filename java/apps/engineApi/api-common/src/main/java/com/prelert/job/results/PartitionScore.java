/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
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
 ***********************************************************/
package com.prelert.job.results;

import java.io.IOException;
import java.util.Objects;

import com.prelert.job.persistence.serialisation.StorageSerialisable;
import com.prelert.job.persistence.serialisation.StorageSerialiser;

public class PartitionScore implements StorageSerialisable
{
    private String m_PartitionFieldValue;
    private String m_PartitionFieldName;
    private double m_AnomalyScore;
    private double m_Probability;
    private boolean m_HadBigNormalisedUpdate;


    public PartitionScore()
    {
        m_HadBigNormalisedUpdate = false;
    }

    public PartitionScore(String fieldValue, double anomalyScore, double probability)
    {
        this();
        m_PartitionFieldValue = fieldValue;
        m_AnomalyScore = anomalyScore;
        m_Probability = probability;
    }

    public double getAnomalyScore()
    {
        return m_AnomalyScore;
    }

    public void setAnomalyScore(double anomalyScore)
    {
        m_AnomalyScore = anomalyScore;
    }

    public String getPartitionFieldName()
    {
        return m_PartitionFieldName;
    }

    public void setPartitionFieldName(String partitionFieldName)
    {
        m_PartitionFieldName = partitionFieldName;
    }

    public String getPartitionFieldValue()
    {
        return m_PartitionFieldValue;
    }

    public void setPartitionFieldValue(String partitionFieldValue)
    {
        m_PartitionFieldValue = partitionFieldValue;
    }

    public double getProbability()
    {
        return m_Probability;
    }

    public void setProbability(double probability)
    {
        this.m_Probability = probability;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(m_PartitionFieldValue, m_AnomalyScore);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (other instanceof PartitionScore == false)
        {
            return false;
        }

        PartitionScore that = (PartitionScore)other;

        // m_HadBigNormalisedUpdate is deliberately excluded from the test
        // as is m_Id, which is generated by the datastore
        return Objects.equals(this.m_PartitionFieldValue, that.m_PartitionFieldValue)
                && (this.m_AnomalyScore == that.m_AnomalyScore);
    }

    @Override
    public void serialise(StorageSerialiser serialiser)
    throws IOException
    {
        serialiser.add(AnomalyRecord.PARTITION_FIELD_VALUE, m_PartitionFieldValue);
        serialiser.add(AnomalyRecord.ANOMALY_SCORE, m_AnomalyScore);
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
}
