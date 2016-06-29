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

package com.prelert.distributed;

import static org.junit.Assert.assertEquals;

import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.prelert.job.manager.JobManager;

public class LocalEngineApiHostsTest
{
    @Mock private JobManager m_JobManager;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testEngineApiHosts()
    {
        LocalEngineApiHosts hosts = new LocalEngineApiHosts(m_JobManager);
        assertEquals(1, hosts.engineApiHosts().size());
    }

    @Test
    public void testHostByRunningJob()
    {
        List<String> runningJobs = Arrays.asList("foo", "bar");
        when(m_JobManager.getRunningJobIds()).thenReturn(runningJobs);
        LocalEngineApiHosts hosts = new LocalEngineApiHosts(m_JobManager);

        Map<String, String> hostByRunningJob = hosts.hostByRunningJob();

        assertEquals(2, hostByRunningJob.size());
        String localHost = hostByRunningJob.get("foo");
        assertEquals(localHost, hostByRunningJob.get("bar"));
    }

    @Test
    public void testHostByScheduledJob()
    {
        List<String> scheduledJobs = Arrays.asList("sc_1", "sc_2");
        when(m_JobManager.getStartedScheduledJobs()).thenReturn(scheduledJobs);
        LocalEngineApiHosts hosts = new LocalEngineApiHosts(m_JobManager);

        Map<String, String> hostByRunningJob = hosts.hostByScheduledJob();

        assertEquals(2, hostByRunningJob.size());
        String localHost = hostByRunningJob.get("sc_1");
        assertEquals(localHost, hostByRunningJob.get("sc_2"));
    }

    @Test
    public void testHostByJob()
    {
        List<String> runningJobs = Arrays.asList("foo", "bar");
        when(m_JobManager.getRunningJobIds()).thenReturn(runningJobs);
        List<String> scheduledJobs = Arrays.asList("sc_1", "sc_2");
        when(m_JobManager.getStartedScheduledJobs()).thenReturn(scheduledJobs);
        LocalEngineApiHosts hosts = new LocalEngineApiHosts(m_JobManager);

        Map<String, String> hostByRunningJob = hosts.hostByJob();

        assertEquals(4, hostByRunningJob.size());
        String localHost = hostByRunningJob.get("foo");
        assertEquals(localHost, hostByRunningJob.get("bar"));
        assertEquals(localHost, hostByRunningJob.get("sc_1"));
        assertEquals(localHost, hostByRunningJob.get("sc_2"));
    }
}
