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
 ************************************************************/

package com.prelert.job.process.params;

import java.util.Objects;

public class InterimResultsParams
{
    private final boolean m_CalcInterim;
    private final TimeRange m_TimeRange;
    private final Long m_AdvanceTimeSeconds;

    private InterimResultsParams(boolean calcInterim, TimeRange timeRange, Long advanceTimeSeconds)
    {
        m_CalcInterim = calcInterim;
        m_TimeRange = Objects.requireNonNull(timeRange);
        m_AdvanceTimeSeconds = advanceTimeSeconds;
    }

    public boolean shouldCalculateInterim()
    {
        return m_CalcInterim;
    }

    public boolean shouldAdvanceTime()
    {
        return m_AdvanceTimeSeconds != null;
    }

    public String getStart()
    {
        return m_TimeRange.getStart();
    }

    public String getEnd()
    {
        return m_TimeRange.getEnd();
    }

    public long getAdvanceTime()
    {
        if (!shouldAdvanceTime())
        {
            throw new IllegalStateException();
        }
        return m_AdvanceTimeSeconds;
    }

    public static Builder newBuilder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private boolean m_CalcInterim;
        private TimeRange m_TimeRange;
        private Long m_AdvanceTimeSeconds;

        private Builder()
        {
            m_CalcInterim = false;
            m_TimeRange = new TimeRange(null, null);
            m_AdvanceTimeSeconds = null;
        }

        public Builder calcInterim(boolean value)
        {
            m_CalcInterim = value;
            return this;
        }

        public Builder forTimeRange(Long startSeconds, Long endSeconds)
        {
            return forTimeRange(new TimeRange(startSeconds, endSeconds));
        }

        public Builder forTimeRange(TimeRange timeRange)
        {
            m_TimeRange = timeRange;
            return this;
        }

        public Builder advanceTime(long targetSeconds)
        {
            m_AdvanceTimeSeconds = targetSeconds;
            return this;
        }

        public InterimResultsParams build()
        {
            return new InterimResultsParams(m_CalcInterim, m_TimeRange, m_AdvanceTimeSeconds);
        }
    }
}
