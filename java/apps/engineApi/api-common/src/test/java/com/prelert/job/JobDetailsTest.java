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

package com.prelert.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import com.prelert.job.transform.TransformConfig;

public class JobDetailsTest
{

    @Test
    public void testConstructor_GivenAnotherJobDetailsAndEmptyJobConfiguration()
    {
        AnalysisConfig anotherAnalysisConfig = mock(AnalysisConfig.class);
        AnalysisLimits anotherAnalysisLimits = mock(AnalysisLimits.class);
        DataDescription anotherDataDescription = mock(DataDescription.class);
        TransformConfig anotherTransformConfig = mock(TransformConfig.class);
        List<TransformConfig> anotherTransforms = new ArrayList<>();
        anotherTransforms.add(anotherTransformConfig);

        JobDetails anotherJobDetails = new JobDetails();
        anotherJobDetails.setId("anotherId");
        anotherJobDetails.setTimeout(10000L);
        anotherJobDetails.setAnalysisConfig(anotherAnalysisConfig);
        anotherJobDetails.setAnalysisLimits(anotherAnalysisLimits);
        anotherJobDetails.setDataDescription(anotherDataDescription);
        anotherJobDetails.setDescription("Another");
        anotherJobDetails.setTransforms(anotherTransforms);

        JobConfiguration jobConfiguration = new JobConfiguration();
        jobConfiguration.setTimeout(null);
        jobConfiguration.setAnalysisConfig(null);
        jobConfiguration.setAnalysisLimits(null);
        jobConfiguration.setDataDescription(null);
        jobConfiguration.setDescription(null);
        jobConfiguration.setTransforms(null);

        JobDetails jobDetails = new JobDetails("thisId", anotherJobDetails, jobConfiguration);

        assertEquals("thisId", jobDetails.getId());
        assertEquals(JobStatus.CLOSED, jobDetails.getStatus());
        assertEquals(10000L, jobDetails.getTimeout());
        assertEquals(anotherAnalysisConfig, jobDetails.getAnalysisConfig());
        assertEquals(anotherAnalysisLimits, jobDetails.getAnalysisLimits());
        assertEquals(anotherDataDescription, jobDetails.getDataDescription());
        assertEquals("Another", jobDetails.getDescription());
        assertEquals(anotherTransforms, jobDetails.getTransforms());
    }

    @Test
    public void testConstructor_GivenAnotherJobDetailsAndOverridingJobConfiguration()
    {
        AnalysisConfig anotherAnalysisConfig = mock(AnalysisConfig.class);
        AnalysisLimits anotherAnalysisLimits = mock(AnalysisLimits.class);
        DataDescription anotherDataDescription = mock(DataDescription.class);
        TransformConfig anotherTransformConfig = mock(TransformConfig.class);
        List<TransformConfig> anotherTransforms = new ArrayList<>();
        anotherTransforms.add(anotherTransformConfig);

        JobDetails anotherJobDetails = new JobDetails();
        anotherJobDetails.setId("anotherId");
        anotherJobDetails.setTimeout(10000L);
        anotherJobDetails.setAnalysisConfig(anotherAnalysisConfig);
        anotherJobDetails.setAnalysisLimits(anotherAnalysisLimits);
        anotherJobDetails.setDataDescription(anotherDataDescription);
        anotherJobDetails.setDescription("Another");
        anotherJobDetails.setTransforms(anotherTransforms);

        AnalysisConfig overridingAnalysisConfig = mock(AnalysisConfig.class);
        AnalysisLimits overridingAnalysisLimits = mock(AnalysisLimits.class);
        DataDescription overridingDataDescription = mock(DataDescription.class);
        TransformConfig overridingTransformConfig = mock(TransformConfig.class);
        List<TransformConfig> overridingTransforms = new ArrayList<>();
        anotherTransforms.add(overridingTransformConfig);

        JobConfiguration jobConfiguration = new JobConfiguration();
        jobConfiguration.setTimeout(5000L);
        jobConfiguration.setAnalysisConfig(overridingAnalysisConfig);
        jobConfiguration.setAnalysisLimits(overridingAnalysisLimits);
        jobConfiguration.setDataDescription(overridingDataDescription);
        jobConfiguration.setDescription("Overriding");
        jobConfiguration.setTransforms(overridingTransforms);

        JobDetails jobDetails = new JobDetails("thisId", anotherJobDetails, jobConfiguration);

        assertEquals("thisId", jobDetails.getId());
        assertEquals(JobStatus.CLOSED, jobDetails.getStatus());
        assertEquals(5000L, jobDetails.getTimeout());
        assertEquals(overridingAnalysisConfig, jobDetails.getAnalysisConfig());
        assertEquals(overridingAnalysisLimits, jobDetails.getAnalysisLimits());
        assertEquals(overridingDataDescription, jobDetails.getDataDescription());
        assertEquals("Overriding", jobDetails.getDescription());
        assertEquals(overridingTransforms, jobDetails.getTransforms());
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
    public void testEquals_GivenEqualJobDetails()
    {
        JobConfiguration jobConfiguration = new JobConfiguration();
        JobDetails jobDetails1 = new JobDetails("foo", jobConfiguration);
        JobDetails jobDetails2 = new JobDetails("foo", jobConfiguration);
        Date createTime = new Date();
        jobDetails1.setCreateTime(createTime);
        jobDetails2.setCreateTime(createTime);

        assertTrue(jobDetails1.equals(jobDetails2));
    }

    @Test
    public void testEquals_GivenJobDetailsWithDifferentIds()
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
    public void testHashCode_GivenEqualJobDetails()
    {
        JobConfiguration jobConfiguration = new JobConfiguration();
        JobDetails jobDetails1 = new JobDetails("foo", jobConfiguration);
        JobDetails jobDetails2 = new JobDetails("foo", jobConfiguration);
        Date createTime = new Date();
        jobDetails1.setCreateTime(createTime);
        jobDetails2.setCreateTime(createTime);

        assertEquals(jobDetails1.hashCode(), jobDetails2.hashCode());
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
