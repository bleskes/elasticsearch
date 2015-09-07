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
 ***********************************************************/

package com.prelert.job.alert.manager;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.UriBuilder;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.prelert.job.UnknownJobException;
import com.prelert.job.alert.Alert;
import com.prelert.job.manager.JobManager;
import com.prelert.job.persistence.JobProvider;
import com.prelert.job.process.exceptions.ClosedJobException;

public class AlertManagerTest {

    @Test
    public void testRegisterRequest() throws UnknownJobException, ClosedJobException
    {
        JobManager jobManager = mock(JobManager.class);
        JobProvider jobProvider = mock(JobProvider.class);

        AlertManager am = new AlertManager(jobProvider, jobManager);

        AsyncResponse response = mock(AsyncResponse.class);
        URI uri = UriBuilder.fromUri("http://testing").build();

        am.registerRequest(response, "foo", uri, 20l, 60.0, 70.0);

        verify(response, times(1)).setTimeout(20l, TimeUnit.SECONDS);
        verify(response, times(1)).setTimeoutHandler(am);
        verify(jobManager, times(1)).addAlertObserver(eq("foo"), any());
    }

    @Test
    public void testRegisterRequest_ClosedJobExceptionThrown() throws UnknownJobException,
            ClosedJobException
    {
        JobManager jobManager = mock(JobManager.class);
        JobProvider jobProvider = mock(JobProvider.class);

        AlertManager am = new AlertManager(jobProvider, jobManager);

        AsyncResponse response = mock(AsyncResponse.class);
        URI uri = UriBuilder.fromUri("http://testing").build();

        ClosedJobException e = new ClosedJobException("foo", "bar");
        doThrow(e).when(jobManager).addAlertObserver(eq("foo"), any());

        am.registerRequest(response, "foo", uri, 20l, 60.0, 70.0);
        verify(response, times(1)).resume(e);
    }

    @Test
    public void testHandleTimeout()
    {
        JobManager jobManager = mock(JobManager.class);
        JobProvider jobProvider = mock(JobProvider.class);

        AlertManager am = new AlertManager(jobProvider, jobManager);
        AsyncResponse response = mock(AsyncResponse.class);

        am.handleTimeout(response);

        ArgumentCaptor<Alert> argument = ArgumentCaptor.forClass(Alert.class);
        verify(response).resume(argument.capture());
        assertTrue(argument.getValue().isTimeout());
    }

}
