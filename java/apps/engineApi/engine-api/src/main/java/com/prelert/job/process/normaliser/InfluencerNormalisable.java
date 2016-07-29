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

package com.prelert.job.process.normaliser;

import java.util.Objects;

import com.prelert.job.results.Influencer;

class InfluencerNormalisable extends AbstractLeafNormalisable
{
    private final Influencer m_Influencer;

    public InfluencerNormalisable(Influencer influencer)
    {
        m_Influencer = Objects.requireNonNull(influencer);
    }

    @Override
    public Level getLevel()
    {
        return Level.INFLUENCER;
    }

    @Override
    public String getPartitionFieldName()
    {
        return null;
    }

    @Override
    public String getPartitionFieldValue()
    {
        return null;
    }

    @Override
    public String getPersonFieldName()
    {
        return m_Influencer.getInfluencerFieldName();
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
        return m_Influencer.getProbability();
    }

    @Override
    public double getNormalisedScore()
    {
        return m_Influencer.getAnomalyScore();
    }

    @Override
    public void setNormalisedScore(double normalisedScore)
    {
        m_Influencer.setAnomalyScore(normalisedScore);
    }

    @Override
    public void setParentScore(double parentScore)
    {
        throw new IllegalStateException("Influencer has no parent");
    }

    @Override
    public void resetBigChangeFlag()
    {
        m_Influencer.resetBigNormalisedUpdateFlag();
    }

    @Override
    public void raiseBigChangeFlag()
    {
        m_Influencer.raiseBigNormalisedUpdateFlag();
    }
}
