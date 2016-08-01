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
package com.prelert.job.persistence;

/**
 * One time query builder for buckets.
 * Sets default values for the following parameters:
 * <ul>
 *     <li>Skip = 0</li>
 *     <li>Take = {@value DEFAULT_TAKE_SIZE}</li>
 *     <li>Expand = false</li>
 *     <li>IncludeInterim = false</li>
 *     <li>anomalyScoreThreshold = 0.0</li>
 *     <li>normalizedProbabilityThreshold = 0.0</li>
 *     <li>epochStart = -1</li>
 *     <li>epochEnd = -1</li>
 * </ul>
 */
public final class BucketsQueryBuilder
{
    public static int DEFAULT_TAKE_SIZE = 100;

    private BucketsQuery m_BucketsQuery = new BucketsQuery();

    public BucketsQueryBuilder skip(int skip)
    {
        m_BucketsQuery.m_Skip = skip;
        return this;
    }

    public BucketsQueryBuilder take(int take)
    {
        m_BucketsQuery.m_Take = take;
        return this;
    }

    public BucketsQueryBuilder expand(boolean expand)
    {
        m_BucketsQuery.m_Expand = expand;
        return this;
    }

    public BucketsQueryBuilder includeInterim(boolean include)
    {
        m_BucketsQuery.m_IncludeInterim = include;
        return this;
    }

    public BucketsQueryBuilder anomalyScoreThreshold(Double anomalyScoreFilter)
    {
        m_BucketsQuery.m_AnomalyScoreFilter = anomalyScoreFilter;
        return this;
    }

    public BucketsQueryBuilder normalizedProbabilityThreshold(Double normalizedProbability)
    {
        m_BucketsQuery.m_NormalizedProbability = normalizedProbability;
        return this;
    }

    public BucketsQueryBuilder epochStart(long startTime)
    {
        m_BucketsQuery.m_EpochStart = startTime;
        return this;
    }

    public BucketsQueryBuilder epochEnd(long endTime)
    {
        m_BucketsQuery.m_EpochEnd = endTime;
        return this;
    }

    public BucketsQueryBuilder.BucketsQuery build()
    {
        return m_BucketsQuery;
    }

    public void clear()
    {
        m_BucketsQuery = new BucketsQueryBuilder.BucketsQuery();
    }

    
    public class BucketsQuery
    {
        private int m_Skip = 0;
        private int m_Take = DEFAULT_TAKE_SIZE;
        private boolean m_Expand =  false;
        private boolean m_IncludeInterim = false;
        private double m_AnomalyScoreFilter = 0.0d;
        private double m_NormalizedProbability = 0.0d;
        private long m_EpochStart = -1;
        private long m_EpochEnd = -1;


        public int getSkip()
        {
            return m_Skip;
        }

        public int getTake()
        {
            return m_Take;
        }

        public boolean isExpand()
        {
            return m_Expand;
        }

        public boolean isIncludeInterim()
        {
            return m_IncludeInterim;
        }

        public double getAnomalyScoreFilter()
        {
            return m_AnomalyScoreFilter;
        }

        public double getNormalizedProbability()
        {
            return m_NormalizedProbability;
        }

        public long getEpochStart()
        {
            return m_EpochStart;
        }

        public long getEpochEnd()
        {
            return m_EpochEnd;
        }
    }
}
