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
package com.prelert.job.detectionrules.verification;

import com.prelert.job.Detector;

enum ScopingLevel
{
    PARTITION(3),
    OVER(2),
    BY(1);

    int m_Level;

    private ScopingLevel(int level)
    {
        m_Level = level;
    }

    boolean isHigherThan(ScopingLevel other)
    {
        return m_Level > other.m_Level;
    }

    static ScopingLevel from(Detector detector, String fieldName)
    {
        if (fieldName.equals(detector.getPartitionFieldName()))
        {
            return ScopingLevel.PARTITION;
        }
        if (fieldName.equals(detector.getOverFieldName()))
        {
            return ScopingLevel.OVER;
        }
        if (fieldName.equals(detector.getByFieldName()))
        {
            return ScopingLevel.BY;
        }
        throw new IllegalArgumentException(
                "fieldName '" + fieldName + "' does not match an analysis field");
    }
}
