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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.prelert.job.results.Bucket;

class BucketNormalisable implements Normalisable
{
    private static final int BUCKET_INFLUENCER = 0;
    private static final int RECORD = 1;
    private static final List<Integer> CHILDREN_TYPES = Arrays.asList(BUCKET_INFLUENCER, RECORD);

    private final Bucket m_Bucket;

    public BucketNormalisable(Bucket bucket)
    {
        m_Bucket = Objects.requireNonNull(bucket);
    }

    @Override
    public boolean isContainerOnly()
    {
        return true;
    }

    @Override
    public Level getLevel()
    {
        return Level.ROOT;
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
        throw new IllegalStateException("Bucket is container only");
    }

    @Override
    public double getNormalisedScore()
    {
        return m_Bucket.getAnomalyScore();
    }

    @Override
    public void setNormalisedScore(double normalisedScore)
    {
        m_Bucket.setAnomalyScore(normalisedScore);
    }

    @Override
    public List<Integer> getChildrenTypes()
    {
        return CHILDREN_TYPES;
    }

    @Override
    public List<Normalisable> getChildren()
    {
        List<Normalisable> children = new ArrayList<>();
        for (Integer type : getChildrenTypes())
        {
            children.addAll(getChildren(type));
        }
        return children;
    }

    @Override
    public List<Normalisable> getChildren(int type)
    {
        List<Normalisable> children = new ArrayList<>();
        switch (type)
        {
            case BUCKET_INFLUENCER:
                m_Bucket.getBucketInfluencers().stream().forEach(
                        influencer -> children.add(new BucketInfluencerNormalisable(influencer)));
                break;
            case RECORD:
                m_Bucket.getRecords().stream().forEach(
                        record -> children.add(new RecordNormalisable(record)));
                break;
            default:
                throw new IllegalArgumentException("Invalid type: " + type);
        }
        return children;
    }

    @Override
    public boolean setMaxChildrenScore(int childrenType, double maxScore)
    {
        double oldScore = 0.0;
        switch (childrenType)
        {
            case BUCKET_INFLUENCER:
                oldScore = m_Bucket.getAnomalyScore();
                m_Bucket.setAnomalyScore(maxScore);
                break;
            case RECORD:
                oldScore = m_Bucket.getMaxNormalizedProbability();
                m_Bucket.setMaxNormalizedProbability(maxScore);
                break;
            default:
                throw new IllegalArgumentException("Invalid type: " + childrenType);
        }
        return maxScore != oldScore;
    }

    @Override
    public void setParentScore(double parentScore)
    {
        throw new IllegalStateException("Bucket has no parent");
    }

    @Override
    public void resetBigChangeFlag()
    {
        m_Bucket.resetBigNormalisedUpdateFlag();
    }

    @Override
    public void raiseBigChangeFlag()
    {
        m_Bucket.raiseBigNormalisedUpdateFlag();
    }
}
