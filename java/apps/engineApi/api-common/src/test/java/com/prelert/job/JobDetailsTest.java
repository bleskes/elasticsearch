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

package com.prelert.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.prelert.job.SchedulerConfig.DataSource;

public class JobDetailsTest
{
    @Test
    public void testConstructor_GivenEmptyJobConfiguration()
    {
        JobConfiguration jobConfiguration = new JobConfiguration();

        JobDetails jobDetails = new JobDetails("foo", jobConfiguration);

        assertEquals("foo", jobDetails.getId());
        assertEquals(JobStatus.CLOSED, jobDetails.getStatus());
        assertNotNull(jobDetails.getCreateTime());
        assertEquals(600L, jobDetails.getTimeout());
        assertNull(jobDetails.getSchedulerStatus());
        assertNull(jobDetails.getAnalysisConfig());
        assertNull(jobDetails.getAnalysisLimits());
        assertNull(jobDetails.getCustomSettings());
        assertNull(jobDetails.getDataDescription());
        assertNull(jobDetails.getDescription());
        assertNull(jobDetails.getEndpoints());
        assertNull(jobDetails.getFinishedTime());
        assertNull(jobDetails.getIgnoreDowntime());
        assertNull(jobDetails.getLastDataTime());
        assertNull(jobDetails.getLocation());
        assertNull(jobDetails.getModelDebugConfig());
        assertNull(jobDetails.getModelSizeStats());
        assertNull(jobDetails.getRenormalizationWindowDays());
        assertNull(jobDetails.getBackgroundPersistInterval());
        assertNull(jobDetails.getModelSnapshotRetentionDays());
        assertNull(jobDetails.getResultsRetentionDays());
        assertNull(jobDetails.getSchedulerConfig());
        assertNull(jobDetails.getTransforms());
        assertNotNull(jobDetails.allFields());
        assertTrue(jobDetails.allFields().isEmpty());
    }

    @Test
    public void testConstructor_GivenJobConfigurationWithIgnoreDowntime()
    {
        JobConfiguration jobConfiguration = new JobConfiguration();
        jobConfiguration.setIgnoreDowntime(IgnoreDowntime.ONCE);

        JobDetails jobDetails = new JobDetails("foo", jobConfiguration);

        assertEquals("foo", jobDetails.getId());
        assertEquals(IgnoreDowntime.ONCE, jobDetails.getIgnoreDowntime());
    }

    @Test
    public void testConstructor_GivenJobConfigurationWithElasticsearchScheduler_ShouldFillDefaults()
    {
        SchedulerConfig schedulerConfig = new SchedulerConfig();
        schedulerConfig.setDataSource(DataSource.ELASTICSEARCH);
        schedulerConfig.setQuery(null);
        JobConfiguration jobConfiguration = new JobConfiguration();
        jobConfiguration.setSchedulerConfig(schedulerConfig);

        JobDetails jobDetails = new JobDetails("foo", jobConfiguration);

        Map<String, Object> schedulerQuery = jobDetails.getSchedulerConfig().getQuery();
        assertNotNull(schedulerQuery);
    }

    @Test
    public void testEquals_GivenSameReference()
    {
        JobDetails jobDetails = new JobDetails();
        assertTrue(jobDetails.equals(jobDetails));
    }

    @Test
    public void testEquals_GivenDifferentClass()
    {
        JobDetails jobDetails = new JobDetails();
        assertFalse(jobDetails.equals("a string"));
    }

    @Test
    public void testEquals_GivenEqualJobDetails() throws URISyntaxException
    {
        ModelSizeStats modelSizeStats = new ModelSizeStats();
        Map<String, URI> endpoints = new HashMap<>();
        endpoints.put("buckets", new URI("http://localhost:8080/buckets"));

        JobDetails jobDetails1 = new JobDetails();
        jobDetails1.setId("foo");
        jobDetails1.setAnalysisConfig(new AnalysisConfig());
        jobDetails1.setAnalysisLimits(new AnalysisLimits());
        jobDetails1.setCounts(new DataCounts());
        jobDetails1.setCreateTime(new Date(0));
        jobDetails1.setCustomSettings(new HashMap<>());
        jobDetails1.setDataDescription(new DataDescription());
        jobDetails1.setDescription("Blah blah");
        jobDetails1.setEndpoints(endpoints);
        jobDetails1.setFinishedTime(new Date(1000));
        jobDetails1.setIgnoreDowntime(IgnoreDowntime.ALWAYS);
        jobDetails1.setLastDataTime(new Date(500));
        jobDetails1.setLocation(new URI("http://localhost:8080/jobs/foo"));
        jobDetails1.setModelDebugConfig(new ModelDebugConfig());
        jobDetails1.setModelSizeStats(modelSizeStats);
        jobDetails1.setRenormalizationWindowDays(60L);
        jobDetails1.setBackgroundPersistInterval(10000L);
        jobDetails1.setModelSnapshotRetentionDays(10L);
        jobDetails1.setResultsRetentionDays(30L);
        jobDetails1.setSchedulerConfig(new SchedulerConfig());
        jobDetails1.setSchedulerStatus(JobSchedulerStatus.STOPPED);
        jobDetails1.setStatus(JobStatus.RUNNING);
        jobDetails1.setTimeout(3600L);
        jobDetails1.setTransforms(Collections.emptyList());


        JobDetails jobDetails2 = new JobDetails();
        jobDetails2.setId("foo");
        jobDetails2.setAnalysisConfig(new AnalysisConfig());
        jobDetails2.setAnalysisLimits(new AnalysisLimits());
        jobDetails2.setCounts(new DataCounts());
        jobDetails2.setCreateTime(new Date(0));
        jobDetails2.setCustomSettings(new HashMap<>());
        jobDetails2.setDataDescription(new DataDescription());
        jobDetails2.setDescription("Blah blah");
        jobDetails2.setEndpoints(endpoints);
        jobDetails2.setFinishedTime(new Date(1000));
        jobDetails2.setIgnoreDowntime(IgnoreDowntime.ALWAYS);
        jobDetails2.setLastDataTime(new Date(500));
        jobDetails2.setLocation(new URI("http://localhost:8080/jobs/foo"));
        jobDetails2.setModelDebugConfig(new ModelDebugConfig());
        jobDetails2.setModelSizeStats(modelSizeStats);
        jobDetails2.setRenormalizationWindowDays(60L);
        jobDetails2.setBackgroundPersistInterval(10000L);
        jobDetails2.setModelSnapshotRetentionDays(10L);
        jobDetails2.setResultsRetentionDays(30L);
        jobDetails2.setSchedulerConfig(new SchedulerConfig());
        jobDetails2.setSchedulerStatus(JobSchedulerStatus.STOPPED);
        jobDetails2.setStatus(JobStatus.RUNNING);
        jobDetails2.setTimeout(3600L);
        jobDetails2.setTransforms(Collections.emptyList());

        assertTrue(jobDetails1.equals(jobDetails2));
        assertTrue(jobDetails2.equals(jobDetails1));
        assertEquals(jobDetails1.hashCode(), jobDetails2.hashCode());
    }

    @Test
    public void testEquals_GivenDifferentIds()
    {
        JobConfiguration jobConfiguration = new JobConfiguration();
        JobDetails jobDetails1 = new JobDetails("foo", jobConfiguration);
        JobDetails jobDetails2 = new JobDetails("bar", jobConfiguration);
        Date createTime = new Date();
        jobDetails1.setCreateTime(createTime);
        jobDetails2.setCreateTime(createTime);

        assertFalse(jobDetails1.equals(jobDetails2));
    }

    @Test
    public void testEquals_GivenDifferentSchedulerStatus()
    {
        JobConfiguration jobConfiguration = new JobConfiguration();
        JobDetails jobDetails1 = new JobDetails("foo", jobConfiguration);
        jobDetails1.setSchedulerStatus(JobSchedulerStatus.STOPPED);
        JobDetails jobDetails2 = new JobDetails("bar", jobConfiguration);
        jobDetails2.setSchedulerStatus(JobSchedulerStatus.STARTED);

        assertFalse(jobDetails1.equals(jobDetails2));
    }

    @Test
    public void testEquals_GivenDifferentRenormalizationWindowDays()
    {
        JobConfiguration jobDetails1 = new JobConfiguration();
        jobDetails1.setRenormalizationWindowDays(3L);
        JobConfiguration jobDetails2 = new JobConfiguration();
        jobDetails2.setRenormalizationWindowDays(4L);

        assertFalse(jobDetails1.equals(jobDetails2));
    }

    @Test
    public void testEquals_GivenDifferentBackgroundPersistInterval()
    {
        JobConfiguration jobDetails1 = new JobConfiguration();
        jobDetails1.setBackgroundPersistInterval(10000L);
        JobConfiguration jobDetails2 = new JobConfiguration();
        jobDetails2.setBackgroundPersistInterval(8000L);

        assertFalse(jobDetails1.equals(jobDetails2));
    }

    @Test
    public void testEquals_GivenDifferentModelSnapshotRetentionDays()
    {
        JobConfiguration jobDetails1 = new JobConfiguration();
        jobDetails1.setModelSnapshotRetentionDays(10L);
        JobConfiguration jobDetails2 = new JobConfiguration();
        jobDetails2.setModelSnapshotRetentionDays(8L);

        assertFalse(jobDetails1.equals(jobDetails2));
    }

    @Test
    public void testEquals_GivenDifferentResultsRetentionDays()
    {
        JobConfiguration jobDetails1 = new JobConfiguration();
        jobDetails1.setResultsRetentionDays(30L);
        JobConfiguration jobDetails2 = new JobConfiguration();
        jobDetails2.setResultsRetentionDays(4L);

        assertFalse(jobDetails1.equals(jobDetails2));
    }

    @Test
    public void testEquals_GivenDifferentCustomSettings()
    {
        JobConfiguration jobDetails1 = new JobConfiguration();
        Map<String, Object> customSettings1 = new HashMap<>();
        customSettings1.put("key1", "value1");
        jobDetails1.setCustomSettings(customSettings1);
        JobConfiguration jobDetails2 = new JobConfiguration();
        Map<String, Object> customSettings2 = new HashMap<>();
        customSettings2.put("key2", "value2");
        jobDetails2.setCustomSettings(customSettings2);

        assertFalse(jobDetails1.equals(jobDetails2));
    }

    @Test
    public void testEquals_GivenDifferentIgnoreDowntime()
    {
        JobDetails job1 = new JobDetails();
        job1.setIgnoreDowntime(IgnoreDowntime.NEVER);
        JobDetails job2 = new JobDetails();
        job2.setIgnoreDowntime(IgnoreDowntime.ONCE);

        assertFalse(job1.equals(job2));
        assertFalse(job2.equals(job1));
    }

    @Test
    public void testToString()
    {
        Date createTime = new Date(1443410000);
        Date lastDataTime = new Date(1443420000);

        JobDetails jobDetails = new JobDetails();
        jobDetails.setId("foo");
        jobDetails.setDescription("blah blah");
        jobDetails.setStatus(JobStatus.RUNNING);
        jobDetails.setCreateTime(createTime);
        jobDetails.setLastDataTime(lastDataTime);

        String expected = "{id:foo description:blah blah status:RUNNING createTime:" + createTime
                + " lastDataTime:" + lastDataTime + "}";

        assertEquals(expected, jobDetails.toString());
    }
}
