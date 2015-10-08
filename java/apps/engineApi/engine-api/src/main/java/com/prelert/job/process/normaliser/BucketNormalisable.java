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

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.prelert.job.results.Bucket;

class BucketNormalisable implements Normalisable
{
    private final Bucket m_Bucket;

    public BucketNormalisable(Bucket bucket)
    {
        m_Bucket = Objects.requireNonNull(bucket);
    }

    @Override
    public Level getLevel()
    {
        return Level.ROOT;
    }

    @Override
    public String getPartitonFieldName()
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
    public double getInitialScore()
    {
        return m_Bucket.getRawAnomalyScore();
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
    public List<Normalisable> getChildren()
    {
        return m_Bucket.getRecords().stream().map(record -> new RecordNormalisable(record))
                .collect(Collectors.toList());
    }

    @Override
    public void setMaxChildrenScore(double maxScore)
    {
        m_Bucket.setMaxNormalizedProbability(maxScore);
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
