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

package com.prelert.rs.resources;

import static com.prelert.job.errorcodes.ErrorCodeMatcher.hasErrorCode;

import static org.junit.Assert.assertTrue;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import java.util.OptionalLong;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;

import com.prelert.job.JobException;
import com.prelert.job.UnknownJobException;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.manager.CannotStartSchedulerException;
import com.prelert.job.manager.NoSuchScheduledJobException;
import com.prelert.rs.data.Acknowledgement;
import com.prelert.rs.exception.InvalidParametersException;

public class ControlTest extends ServiceTest
{
    @Rule
    public ExpectedException m_ExpectedException = ExpectedException.none();

    private Control m_Control;

    @Before
    public void setUp() throws UnknownJobException
    {
        m_Control = new Control();
        configureService(m_Control);
    }

    @Test
    public void testStartScheduledJob_GivenEndEarlierThanStart()
            throws CannotStartSchedulerException, NoSuchScheduledJobException, UnknownJobException
    {
        m_ExpectedException.expect(InvalidParametersException.class);
        m_ExpectedException.expectMessage(
                "Invalid time range: end time '2015-01-01T00:00:00Z' is earlier than start time '2016-01-01T00:00:00Z'");
        m_ExpectedException.expect(hasErrorCode(ErrorCodes.END_DATE_BEFORE_START_DATE));

        m_Control.startScheduledJob("foo", "2016-01-01T00:00:00Z", "2015-01-01T00:00:00Z");
    }

    @Test
    public void testStartScheduledJob_GivenEndEqualToStart() throws JobException
    {
        m_ExpectedException.expect(InvalidParametersException.class);
        m_ExpectedException.expectMessage(
                "Invalid time range: end time '2016-01-01T00:00:00Z' is earlier than start time '2016-01-01T00:00:00Z'");
        m_ExpectedException.expect(hasErrorCode(ErrorCodes.END_DATE_BEFORE_START_DATE));

        m_Control.startScheduledJob("foo", "2016-01-01T00:00:00Z", "2016-01-01T00:00:00Z");
    }

    @Test
    public void testStartScheduledJob_GivenDefaultStartEndTimes() throws JobException
    {
        Response response = m_Control.startScheduledJob("foo", "", "");

        Acknowledgement acknowledgement = (Acknowledgement) response.getEntity();
        assertTrue(acknowledgement.getAcknowledgement());
        verify(jobManager()).startJobScheduler("foo", 0, OptionalLong.empty());
    }

    @Test
    public void testStartScheduledJob_GivenValidStartEndTimes() throws JobException
    {
        Response response = m_Control.startScheduledJob(
                "foo", "2016-01-01T00:00:00Z", "2016-02-18T20:00:00Z");

        Acknowledgement acknowledgement = (Acknowledgement) response.getEntity();
        assertTrue(acknowledgement.getAcknowledgement());
        verify(jobManager()).startJobScheduler("foo", 1451606400000L, OptionalLong.of(1455825600000L));
    }

    @Test
    public void testStartScheduledJob_GivenValidStartIsNow() throws JobException
    {
        long now = System.currentTimeMillis();

        Response response = m_Control.startScheduledJob("foo", "now", "");

        Acknowledgement acknowledgement = (Acknowledgement) response.getEntity();
        assertTrue(acknowledgement.getAcknowledgement());
        ArgumentCaptor<Long> startTimeCaptor = ArgumentCaptor.forClass(Long.class);
        verify(jobManager()).startJobScheduler(
                eq("foo"), startTimeCaptor.capture(), eq(OptionalLong.empty()));
        assertTrue(startTimeCaptor.getValue() >= now);
        assertTrue(startTimeCaptor.getValue() < now + 300);
    }

    @Test
    public void testStopScheduledJob() throws JobException
    {
        Response response = m_Control.stopScheduledJob("foo");

        Acknowledgement acknowledgement = (Acknowledgement) response.getEntity();
        assertTrue(acknowledgement.getAcknowledgement());
        verify(jobManager()).stopJobScheduler("foo");
    }
}
