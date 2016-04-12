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

package com.prelert.job.manager;

import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.prelert.job.JobException;
import com.prelert.job.UnknownJobException;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.process.exceptions.NativeProcessRunException;

public class JobTimeoutsTest
{
    @Mock private JobTimeouts.JobCloser m_JobCloser;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testStartTimeout() throws InterruptedException, UnknownJobException,
            JobInUseException, NativeProcessRunException
    {
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                latch.countDown();
                return null;
            }
        }).when(m_JobCloser).closeJob("foo");

        JobTimeouts jobTimeouts = new JobTimeouts(m_JobCloser);

        long start = System.currentTimeMillis();
        jobTimeouts.startTimeout("foo", Duration.ofMillis(100));
        latch.await();
        long end = System.currentTimeMillis();

        verify(m_JobCloser).closeJob("foo");
        assertTrue(end - start > 95);
    }

    @Test
    public void testStartTimeout_GivenZero() throws InterruptedException, JobException
    {
        JobTimeouts jobTimeouts = new JobTimeouts(m_JobCloser);

        jobTimeouts.startTimeout("foo", Duration.ZERO);

        Thread.sleep(100);

        verify(m_JobCloser, never()).closeJob("foo");
    }

    @Test
    public void testStartTimeout_GivenNegative() throws InterruptedException, JobException
    {
        JobTimeouts jobTimeouts = new JobTimeouts(m_JobCloser);

        jobTimeouts.startTimeout("foo", Duration.ofSeconds(-1));

        Thread.sleep(100);

        verify(m_JobCloser, never()).closeJob("foo");
    }

    @Test
    public void testStartTimeout_GivenCloseThrowsJobInUseExceptionOnce() throws InterruptedException,
            UnknownJobException, JobInUseException, NativeProcessRunException
    {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger callCount = new AtomicInteger(0);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                int callId = callCount.incrementAndGet();
                if (callId == 1)
                {
                    throw new JobInUseException("in use", ErrorCodes.UNKNOWN_ERROR);
                }
                latch.countDown();
                return null;
            }
        }).when(m_JobCloser).closeJob("foo");

        JobTimeouts jobTimeouts = new JobTimeouts(m_JobCloser, 100);

        long start = System.currentTimeMillis();
        jobTimeouts.startTimeout("foo", Duration.ofMillis(100));
        latch.await();
        long end = System.currentTimeMillis();

        verify(m_JobCloser, times(2)).closeJob("foo");
        assertTrue(end - start > 195);
    }

    @Test
    public void testStopTimeout_GivenNoTimeout()
    {
        JobTimeouts jobTimeouts = new JobTimeouts(m_JobCloser);
        jobTimeouts.stopTimeout("foo");
    }

    @Test
    public void testStopTimeout_GivenTimeout() throws InterruptedException
    {
        JobTimeouts jobTimeouts = new JobTimeouts(m_JobCloser);
        jobTimeouts.startTimeout("foo", Duration.ofMillis(100));

        jobTimeouts.stopTimeout("foo");

        Thread.sleep(200);

        Mockito.verifyNoMoreInteractions(m_JobCloser);
    }

    @Test
    public void testShutdown() throws UnknownJobException, JobInUseException,
            NativeProcessRunException
    {
        JobTimeouts jobTimeouts = new JobTimeouts(m_JobCloser);
        jobTimeouts.startTimeout("foo", Duration.ofSeconds(1));
        jobTimeouts.startTimeout("bar", Duration.ofSeconds(5));
        jobTimeouts.startTimeout("no_timeout", Duration.ofSeconds(0));
        jobTimeouts.startTimeout("no_timeout_2", Duration.ofSeconds(-1));

        jobTimeouts.shutdown();

        verify(m_JobCloser).closeJob("foo");
        verify(m_JobCloser).closeJob("bar");
        verify(m_JobCloser).closeJob("no_timeout");
        verify(m_JobCloser).closeJob("no_timeout_2");
    }

    @Test
    public void testShutdown_GivenCloseJobThrowsJobInUseExceptionOnce()
            throws UnknownJobException, JobInUseException, NativeProcessRunException
    {
        AtomicInteger callCount = new AtomicInteger(0);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                int callId = callCount.incrementAndGet();
                if (callId == 1)
                {
                    throw new JobInUseException("in use", ErrorCodes.UNKNOWN_ERROR);
                }
                return null;
            }
        }).when(m_JobCloser).closeJob("foo");

        JobTimeouts jobTimeouts = new JobTimeouts(m_JobCloser, 100);
        jobTimeouts.startTimeout("foo", Duration.ofSeconds(1));
        jobTimeouts.startTimeout("bar", Duration.ofSeconds(5));

        jobTimeouts.shutdown();

        verify(m_JobCloser, times(2)).closeJob("foo");
        verify(m_JobCloser).closeJob("bar");
    }

    @Test
    public void testShutdown_GivenCloseJobThrowsUnknownJobException()
            throws UnknownJobException, JobInUseException, NativeProcessRunException
    {
        doThrow(new UnknownJobException("foo")).when(m_JobCloser).closeJob("foo");

        JobTimeouts jobTimeouts = new JobTimeouts(m_JobCloser, 100);
        jobTimeouts.startTimeout("foo", Duration.ofSeconds(1));

        jobTimeouts.shutdown();

        verify(m_JobCloser).closeJob("foo");
    }

    @Test
    public void testShutdown_GivenCloseJobThrowsNativeProcessRunException()
            throws UnknownJobException, JobInUseException, NativeProcessRunException
    {
        doThrow(new NativeProcessRunException("some error")).when(m_JobCloser).closeJob("foo");

        JobTimeouts jobTimeouts = new JobTimeouts(m_JobCloser, 100);
        jobTimeouts.startTimeout("foo", Duration.ofSeconds(1));

        jobTimeouts.shutdown();

        verify(m_JobCloser).closeJob("foo");
    }
}
