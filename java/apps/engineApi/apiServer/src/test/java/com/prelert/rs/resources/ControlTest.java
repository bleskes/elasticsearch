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

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;

import com.prelert.job.UnknownJobException;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.manager.CannotStartSchedulerWhileItIsStoppingException;
import com.prelert.job.manager.NoSuchScheduledJobException;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.rs.data.Acknowledgement;

public class ControlTest extends ServiceTest
{
    private Control m_Control;

    @Before
    public void setUp() throws UnknownJobException
    {
        m_Control = new Control();
        configureService(m_Control);
    }

    @Test
    public void testStartScheduledJob()
            throws CannotStartSchedulerWhileItIsStoppingException, NoSuchScheduledJobException
    {
        Response response = m_Control.startScheduledJob("foo");

        Acknowledgement acknowledgement = (Acknowledgement) response.getEntity();
        assertTrue(acknowledgement.getAcknowledgement());
        verify(jobManager()).startExistingJobScheduler("foo");
    }

    @Test
    public void testStopScheduledJob() throws UnknownJobException,
            CannotStartSchedulerWhileItIsStoppingException, NoSuchScheduledJobException,
            NativeProcessRunException, JobInUseException
    {
        Response response = m_Control.stopScheduledJob("foo");

        Acknowledgement acknowledgement = (Acknowledgement) response.getEntity();
        assertTrue(acknowledgement.getAcknowledgement());
        verify(jobManager()).stopExistingJobScheduler("foo");
    }
}
