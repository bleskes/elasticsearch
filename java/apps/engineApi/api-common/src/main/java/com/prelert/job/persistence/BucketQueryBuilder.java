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

import com.prelert.utils.Strings;

/**
 * One time query builder for a single buckets.
 * <ul>
 *     <li>Timestamp (Required) - Timestamp of the bucket</li>
 *     <li>Expand- Include anomaly records. Default= false</li>
 *     <li>IncludeInterim- Include interim results. Default = false</li>
 *     <li>partitionValue Set the bucket's max normalised probabiltiy to this
 * partiton field value's max normalised probability. Default = null</li>
 * </ul>
 */
public final class BucketQueryBuilder
{
    public static int DEFAULT_TAKE_SIZE = 100;

    private BucketQuery m_BucketQuery;

    public BucketQueryBuilder(long timestampMillis)
    {
        m_BucketQuery = new BucketQuery(timestampMillis);
    }

    public BucketQueryBuilder expand(boolean expand)
    {
        m_BucketQuery.m_Expand = expand;
        return this;
    }

    public BucketQueryBuilder includeInterim(boolean include)
    {
        m_BucketQuery.m_IncludeInterim = include;
        return this;
    }

    /**
     * partitionValue must be non null and not empty else it
     * is not set
     * @param partitionValue
     * @return
     */
    public BucketQueryBuilder partitionValue(String partitionValue)
    {
        if (!Strings.isNullOrEmpty(partitionValue))
        {
            m_BucketQuery.m_PartitionValue = partitionValue;
        }
        return this;
    }

    public BucketQueryBuilder.BucketQuery build()
    {
        return m_BucketQuery;
    }

    public class BucketQuery
    {
        private long m_TimestampMillis;
        private boolean m_Expand =  false;
        private boolean m_IncludeInterim = false;
        private String m_PartitionValue = null;

        public BucketQuery(long timestamp)
        {
            m_TimestampMillis = timestamp;
        }

        public long getTimestamp()
        {
            return m_TimestampMillis;
        }

        public boolean isIncludeInterim()
        {
            return m_IncludeInterim;
        }

        public boolean isExpand()
        {
            return m_Expand;
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
            return Objects.hash(m_TimestampMillis, m_Expand, m_IncludeInterim,
                        m_PartitionValue);
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

            BucketQuery other = (BucketQuery) obj;
            return this.m_TimestampMillis == other.m_TimestampMillis &&
                   this.m_Expand == other.m_Expand &&
                   this.m_IncludeInterim == other.m_IncludeInterim &&
                   this.m_PartitionValue == other.m_PartitionValue;
        }

    }
}
