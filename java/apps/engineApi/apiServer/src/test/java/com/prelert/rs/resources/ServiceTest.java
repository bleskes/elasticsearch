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

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.uri.internal.JerseyUriBuilder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.prelert.job.alert.manager.AlertManager;
import com.prelert.job.audit.Auditor;
import com.prelert.job.manager.JobManager;
import com.prelert.job.reader.JobDataReader;

/**
 * Test base class for testing the REST end-points.
 */
public class ServiceTest
{
    protected static final URI BASE_URI = new JerseyUriBuilder().uri("http://localhost/test").build();

    private final JobManager m_JobManager;
    private final JobDataReader m_JobReader;
    private final Auditor m_Auditor;
    private final AlertManager m_AlertManager;
    private final UriInfo m_UriInfo;

    protected ServiceTest()
    {
        m_JobManager = mock(JobManager.class);
        m_JobReader = mock(JobDataReader.class);
        m_Auditor = mock(Auditor.class);
        when(m_JobManager.audit(anyString())).thenReturn(m_Auditor);
        m_AlertManager = mock(AlertManager.class);

        m_UriInfo = mock(UriInfo.class);
        when(m_UriInfo.getBaseUri()).thenReturn(BASE_URI);
        when(m_UriInfo.getBaseUriBuilder()).thenAnswer(new Answer<UriBuilder>()
        {
            @Override
            public UriBuilder answer(InvocationOnMock invocation) throws Throwable
            {
                return UriBuilder.fromUri(BASE_URI);
            }
        });
    }

    protected void configureService(ResourceWithJobManager service)
    {
        Set<Object> singletons = new HashSet<>();
        singletons.add(m_JobManager);
        singletons.add(m_AlertManager);
        singletons.add(m_JobReader);

        Application application = mock(Application.class);
        when(application.getSingletons()).thenReturn(singletons);
        service.setApplication(application);
        service.setUriInfo(m_UriInfo);
    }

    protected JobManager jobManager()
    {
        return m_JobManager;
    }

    protected JobDataReader jobReader()
    {
        return m_JobReader;
    }

    protected Auditor auditor()
    {
        return m_Auditor;
    }

    protected AlertManager alertManager()
    {
        return m_AlertManager;
    }
}
