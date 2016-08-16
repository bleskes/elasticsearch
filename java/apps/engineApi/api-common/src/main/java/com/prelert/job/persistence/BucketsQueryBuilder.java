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

import java.util.Objects;

/**
 * One time query builder for buckets.
 * <ul>
 *     <li>Skip- Skip the first N Buckets. This parameter is for paging
 * if not required set to 0. Default = 0</li>
 *     <li>Take- Take only this number of Buckets. Default = {@value DEFAULT_TAKE_SIZE}</li>
 *     <li>Expand- Include anomaly records. Default= false</li>
 *     <li>IncludeInterim- Include interim results. Default = false</li>
 *     <li>anomalyScoreThreshold- Return only buckets with an anomalyScore >=
 * this value. Default = 0.0</li>
 *     <li>normalizedProbabilityThreshold- Return only buckets with a maxNormalizedProbability >=
 * this value. Default = 0.0</li>
 *     <li>epochStart- The start bucket time. A bucket with this timestamp will be
 * included in the results. If 0 all buckets up to <code>endEpochMs</code>
 * are returned. Default = -1</li>
 *     <li>epochEnd- The end bucket timestamp buckets up to but NOT including this
 * timestamp are returned. If 0 all buckets from <code>startEpochMs</code>
 * are returned. Default = -1</li>
 *     <li>partitionValue Set the bucket's max normalised probabiltiy to this
 * partiton field value's max normalised probability. Default = null</li>
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

    public BucketsQueryBuilder partitionValue(String partitionValue)
    {
        m_BucketsQuery.m_PartitionValue = partitionValue;
        return this;
    }


    /**
     * If startTime <= 0 the parameter is not set
     * @param startTime
     * @return
     */
    public BucketsQueryBuilder epochStart(long startTime)
    {
        if (startTime > 0)
        {
            m_BucketsQuery.m_EpochStart = startTime;
        }
        return this;
    }

    /**
     * If endTime <= 0 the parameter is not set
     * @param endTime
     * @return
     */
    public BucketsQueryBuilder epochEnd(long endTime)
    {
        if (endTime > 0)
        {
            m_BucketsQuery.m_EpochEnd = endTime;
        }
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
        private String m_PartitionValue = null;

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

        /**
         * @return Null if not set
         */
        public String getPartitionValue()
        {
            return m_PartitionValue;
        }

        @Override
        public int hashCode() {
            return Objects.hash(m_Skip, m_Take, m_Expand, m_IncludeInterim,
                        m_AnomalyScoreFilter, m_NormalizedProbability,
                        m_EpochStart, m_EpochEnd, m_PartitionValue);
        }


        @Override
        public boolean equals(Object obj) {
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

            BucketsQuery other = (BucketsQuery) obj;
            return this.m_Skip == other.m_Skip &&
                   this.m_Take == other.m_Take &&
                   this.m_Expand == other.m_Expand &&
                   this.m_IncludeInterim == other.m_IncludeInterim &&
                   this.m_EpochStart == other.m_EpochStart &&
                   this.m_EpochStart == other.m_EpochStart &&
                   this.m_AnomalyScoreFilter == other.m_AnomalyScoreFilter &&
                   this.m_NormalizedProbability == other.m_NormalizedProbability &&
                   this.m_PartitionValue == other.m_PartitionValue;
        }

    }
}
