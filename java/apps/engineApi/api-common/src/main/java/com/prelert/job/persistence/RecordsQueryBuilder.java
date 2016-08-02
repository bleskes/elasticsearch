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
 * One time query builder for records.
 * Sets default values for the following parameters:
 * <ul>
 *     <li>Skip = 0</li>
 *     <li>Take = {@value DEFAULT_TAKE_SIZE}</li>
 *     <li>IncludeInterim = false</li>
 *     <li>SortDescending = true</li>
 *     <li>anomalyScoreThreshold = 0.0</li>
 *     <li>normalizedProbabilityThreshold = 0.0</li>
 *     <li>epochStart = -1</li>
 *     <li>epochEnd = -1</li>
 * </ul>
 */
public final class RecordsQueryBuilder
{
    public static int DEFAULT_TAKE_SIZE = 100;

    private RecordsQuery m_RecordsQuery = new RecordsQuery();

    public RecordsQueryBuilder skip(int skip)
    {
        m_RecordsQuery.m_Skip = skip;
        return this;
    }

    public RecordsQueryBuilder take(int take)
    {
        m_RecordsQuery.m_Take = take;
        return this;
    }

    public RecordsQueryBuilder epochStart(long startTime)
    {
        m_RecordsQuery.m_EpochStart = startTime;
        return this;
    }

    public RecordsQueryBuilder epochEnd(long endTime)
    {
        m_RecordsQuery.m_EpochEnd = endTime;
        return this;
    }

    public RecordsQueryBuilder includeInterim(boolean include)
    {
        m_RecordsQuery.m_IncludeInterim = include;
        return this;
    }

    public RecordsQueryBuilder sortField(String fieldname)
    {
        m_RecordsQuery.m_SortField = fieldname;
        return this;
    }

    public RecordsQueryBuilder sortDescending(boolean sortDescending)
    {
        m_RecordsQuery.m_SortDescending = sortDescending;
        return this;
    }

    public RecordsQueryBuilder anomalyScoreFilter(double anomalyScoreFilter)
    {
        m_RecordsQuery.m_AnomalyScoreFilter = anomalyScoreFilter;
        return this;
    }

    public RecordsQueryBuilder normalizedProbability(double normalizedProbability)
    {
        m_RecordsQuery.m_NormalizedProbability = normalizedProbability;
        return this;
    }

    public RecordsQueryBuilder partitionFieldValue(String partitionFieldValue)
    {
        m_RecordsQuery.m_PartitionFieldValue = partitionFieldValue;
        return this;
    }

    public RecordsQuery build()
    {
        return m_RecordsQuery;
    }

    public void clear()
    {
        m_RecordsQuery = new RecordsQuery();
    }

    public class RecordsQuery
    {
        private int m_Skip = 0;
        private int m_Take = DEFAULT_TAKE_SIZE;
        private boolean m_IncludeInterim = false;
        private String m_SortField;
        private boolean m_SortDescending = true;
        private double m_AnomalyScoreFilter = 0.0d;
        private double m_NormalizedProbability = 0.0d;
        private String m_PartitionFieldValue;
        private long m_EpochStart = -1;
        private long m_EpochEnd = -1;


        public int getTake()
        {
            return m_Take;
        }

        public boolean isIncludeInterim()
        {
            return m_IncludeInterim;
        }

        public String getSortField()
        {
            return m_SortField;
        }

        public boolean isSortDescending()
        {
            return m_SortDescending;
        }

        public double getAnomalyScoreThreshold()
        {
            return m_AnomalyScoreFilter;
        }

        public double getNormalizedProbabilityThreshold()
        {
            return m_NormalizedProbability;
        }

        public String getPartitionFieldValue()
        {
            return m_PartitionFieldValue;
        }

        public int getSkip()
        {
            return m_Skip;
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


