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

package com.prelert.rs.resources;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import com.prelert.job.manager.JobManager;

/**
 * Test base class for testing the REST end-points.
 */
public class ServiceTest
{
    protected JobManager m_JobManager;

    protected ServiceTest()
    {
        m_JobManager = mock(JobManager.class);
    }

    protected void configureService(ResourceWithJobManager service)
    {
        Set<Object> singletons = new HashSet<>();
        singletons.add(m_JobManager);
        Application application = mock(Application.class);
        when(application.getSingletons()).thenReturn(singletons);
        service.setApplication(application);
    }

    protected JobManager jobManager()
    {
        return m_JobManager;
    }
}
