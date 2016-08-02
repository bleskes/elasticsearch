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
package com.prelert.rs.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.prelert.rs.data.EngineStatus.JobStats;

public class EngineStatusTest {

    @Test
    public void testEngineStatus()
    {
        JobStats stats1 = new JobStats();
        JobStats stats2 = new JobStats(55L, "Sharknado", 65L);
        assertFalse(stats1.equals(stats2));
        stats1.setMemoryStatus("Sharknado");
        stats1.setModelBytes(55L);
        stats1.setUptimeSeconds(65L);
        assertEquals(stats1, stats2);
        assertEquals((Long)55L, stats2.getModelBytes());
        assertEquals((Long)65L, stats2.getUptimeSeconds());
        assertEquals("Sharknado", stats2.getMemoryStatus());

        EngineStatus e1 = new EngineStatus();
        EngineStatus e2 = new EngineStatus();
        assertEquals(e1, e2);

        Map<String, JobStats> jobMap = new HashMap<>();
        jobMap.put("Best film ever",  stats1);
        stats1.setMemoryStatus("Jaws");
        jobMap.put("Terrible movie", stats2);

        e1.setRunningJobs(jobMap);
        e2.setRunningJobs(jobMap);
        assertEquals(jobMap, e1.getRunningJobs());

        List<String> l = Arrays.asList("Jurassic Park", "Predator", "Legally Blonde", "Terminator", "The Seventh Seal");
        e1.setStartedScheduledJobs(l);
        e2.setStartedScheduledJobs(l);
        assertEquals(l, e1.getStartedScheduledJobs());

        e1.setAverageCpuLoad(55555.6);
        e2.setAverageCpuLoad(55555.6);
        assertEquals(55555.6, e1.getAverageCpuLoad(), 0.0000001);

        e1.setJvmHeapMemoryUsage(987654321L);
        e2.setJvmHeapMemoryUsage(987654321L);
        assertEquals((Long)987654321L, e1.getJvmHeapMemoryUsage());

        l = Arrays.asList("Snakes on a plane", "Disaster Movie", "Catwoman", "The Last Airbender");
        e1.setEngineHosts(l);
        e2.setEngineHosts(l);
        assertEquals(l, e1.getEngineHosts());

        Map<String, String> m = new HashMap<>();
        m.put("Alec", "Baldwin");
        m.put("Natalie", "Portman");
        m.put("Matt", "Damon");
        m.put("Keira", "Knightley");
        e1.setHostByJob(m);
        e2.setHostByJob(m);
        assertEquals(m, e1.getHostByJob());

        m.put("Elizabeth", "Debicki");
        e1.setDataStoreConnection(m);
        e2.setDataStoreConnection(m);
        assertEquals(m, e1.getDataStoreConnection());

        assertEquals(e1, e2);
    }
}
