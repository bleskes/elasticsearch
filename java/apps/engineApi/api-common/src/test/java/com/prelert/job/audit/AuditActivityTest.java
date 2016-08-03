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
package com.prelert.job.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;

public class AuditActivityTest
{
    private long m_StartMillis;

    @Before
    public void setUp()
    {
        m_StartMillis = System.currentTimeMillis();
    }

    @Test
    public void testDefaultConstructor()
    {
        AuditActivity activity = new AuditActivity();
        assertEquals(0, activity.getTotalJobs());
        assertEquals(0, activity.getTotalDetectors());
        assertEquals(0, activity.getRunningJobs());
        assertEquals(0, activity.getRunningDetectors());
        assertNull(activity.getTimestamp());
    }

    @Test
    public void testNewActivity()
    {
        AuditActivity activity = AuditActivity.newActivity(10, 100, 5, 50);
        assertEquals(10, activity.getTotalJobs());
        assertEquals(100, activity.getTotalDetectors());
        assertEquals(5, activity.getRunningJobs());
        assertEquals(50, activity.getRunningDetectors());
        assertDateBetweenStartAndNow(activity.getTimestamp());
    }

    private void assertDateBetweenStartAndNow(Date timestamp)
    {
        long timestampMillis = timestamp.getTime();
        assertTrue(timestampMillis >= m_StartMillis);
        assertTrue(timestampMillis <= System.currentTimeMillis());
    }
}
