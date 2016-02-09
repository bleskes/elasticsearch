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

package com.prelert.utils.scheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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
    public void testTaskRunsThriceGiven50MsPeriodAndWaitingFor200Ms()
    {
        TaskScheduler scheduler = new TaskScheduler(() -> m_TaskCount.incrementAndGet(),
                () -> LocalDateTime.now().plus(50, ChronoUnit.MILLIS));
        scheduler.start();

        try
        {
            Thread.sleep(200);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        scheduler.stop(1, TimeUnit.SECONDS);

        assertTrue(m_TaskCount.get() >= 3);
        assertTrue(m_TaskCount.get() <= 4);
    }

    @Test
    public void testStop_BlocksUntilRunningTaskTerminates() throws InterruptedException
    {
        CountDownLatch firstTaskStartedLatch = new CountDownLatch(1);
        AtomicLong firstTaskStart = new AtomicLong(0);
        AtomicLong end = new AtomicLong(0);

        Runnable task = () -> {
            int id = m_TaskCount.incrementAndGet();
            long now = System.currentTimeMillis();
            if (id == 1)
            {
                firstTaskStart.getAndSet(now);
                firstTaskStartedLatch.countDown();
            }
            while (now - firstTaskStart.get() <= 100)
            {
                now = System.currentTimeMillis();
            }
            end.getAndSet(now);
        };

        TaskScheduler scheduler = new TaskScheduler(task, () -> LocalDateTime.now());
        scheduler.start();
        firstTaskStartedLatch.await();

        assertTrue(scheduler.stop(1, TimeUnit.SECONDS));

        assertTrue(System.currentTimeMillis() >= end.get());
        assertEquals(1, m_TaskCount.get());
        assertTrue(end.get() - firstTaskStart.get() >= 100);
    }
}
