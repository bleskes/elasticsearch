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

package com.prelert.utils.scheduler;

import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

public class TaskSchedulerTest
{
    private AtomicInteger m_TaskCount;

    @Before
    public void setUp()
    {
        m_TaskCount = new AtomicInteger(0);
    }

    @Test
    public void testTaskRunsTwiceGivenOneSecondPeriodAndWaitingForBitMoreThanTwoSeconds()
    {
        TaskScheduler scheduler = new TaskScheduler(() -> m_TaskCount.incrementAndGet(),
                () -> LocalDateTime.now().plusSeconds(1L));
        scheduler.start();

        try
        {
            Thread.sleep(2100);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        assertEquals(2, m_TaskCount.get());
    }
}
