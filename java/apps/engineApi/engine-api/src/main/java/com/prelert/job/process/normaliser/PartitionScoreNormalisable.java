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
package com.prelert.job.process.normaliser;

import java.util.Objects;

import com.prelert.job.results.PartitionScore;

public class PartitionScoreNormalisable extends AbstractLeafNormalisable
{
    private final PartitionScore m_Score;

    public PartitionScoreNormalisable(PartitionScore score)
    {
        m_Score = Objects.requireNonNull(score);
    }

    @Override
    public Level getLevel()
    {
        return Level.PARTITION;
    }

    @Override
    public String getPartitionFieldName()
    {
        return m_Score.getPartitionFieldName();
    }

    @Override
    public String getPartitionFieldValue()
    {
        return m_Score.getPartitionFieldValue();
    }

    @Override
    public String getPersonFieldName()
    {
        return null;
    }

    @Override
    public String getFunctionName()
    {
        return null;
    }

    @Override
    public String getValueFieldName()
    {
        return null;
    }

    @Override
    public double getProbability()
    {
        return m_Score.getProbability();
    }

    @Override
    public double getNormalisedScore()
    {
        return m_Score.getAnomalyScore();
    }

    @Override
    public void setNormalisedScore(double normalisedScore)
    {
        m_Score.setAnomalyScore(normalisedScore);
    }

    @Override
    public void setParentScore(double parentScore)
    {
        // Do nothing as it is not holding the parent score.
    }

    @Override
    public void resetBigChangeFlag()
    {
        m_Score.resetBigNormalisedUpdateFlag();
    }

    @Override
    public void raiseBigChangeFlag()
    {
        m_Score.raiseBigNormalisedUpdateFlag();
    }
}
