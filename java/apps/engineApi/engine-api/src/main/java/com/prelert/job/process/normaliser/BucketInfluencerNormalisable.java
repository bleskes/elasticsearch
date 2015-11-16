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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.prelert.job.results.BucketInfluencer;

class BucketInfluencerNormalisable implements Normalisable
{
    private final BucketInfluencer m_Influencer;

    public BucketInfluencerNormalisable(BucketInfluencer influencer)
    {
        m_Influencer = Objects.requireNonNull(influencer);
    }

    @Override
    public boolean isContainerOnly()
    {
        return false;
    }

    @Override
    public Level getLevel()
    {
        return BucketInfluencer.BUCKET_TIME.equals(m_Influencer.getInfluencerFieldName()) ?
                Level.ROOT : Level.BUCKET_INFLUENCER;
    }

    @Override
    public String getPartitonFieldName()
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
    public double getInitialScore()
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
    public List<Integer> getChildrenTypes()
    {
        return Collections.emptyList();
    }

    @Override
    public List<Normalisable> getChildren()
    {
        return Collections.emptyList();
    }

    @Override
    public List<Normalisable> getChildren(int type)
    {
        throw new IllegalStateException("BucketInfluencer has no children");
    }

    @Override
    public boolean setMaxChildrenScore(int childrenType, double maxScore)
    {
        throw new IllegalStateException("BucketInfluencer has no children");
    }

    @Override
    public void setParentScore(double parentScore)
    {
        // Do nothing as it is not holding the parent score.
    }

    @Override
    public void resetBigChangeFlag()
    {
        // Do nothing
    }

    @Override
    public void raiseBigChangeFlag()
    {
        // Do nothing
    }
}
