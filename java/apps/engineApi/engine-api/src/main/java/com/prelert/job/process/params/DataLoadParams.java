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

package com.prelert.job.process.params;

import java.util.Objects;

public class DataLoadParams
{
    private final boolean m_IsPersisting;
    private final TimeRange m_ResetTimeRange;
    private final boolean m_IgnoreDowntime;

    public DataLoadParams(boolean isPersisting, TimeRange resetTimeRange)
    {
        this(isPersisting, resetTimeRange, false);
    }

    public DataLoadParams(boolean isPersisting, TimeRange resetTimeRange, boolean ignoreDowntime)
    {
        m_IsPersisting = isPersisting;
        m_ResetTimeRange = Objects.requireNonNull(resetTimeRange);
        m_IgnoreDowntime = ignoreDowntime;
    }

    public boolean isPersisting()
    {
        return m_IsPersisting;
    }

    public boolean isResettingBuckets()
    {
        return !getStart().isEmpty();
    }

    public String getStart()
    {
        return m_ResetTimeRange.getStart();
    }

    public String getEnd()
    {
        return m_ResetTimeRange.getEnd();
    }

    public boolean isIgnoreDowntime()
    {
        return m_IgnoreDowntime;
    }
}

