/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobConfigurationException;

/**
 * Tests the configured fields in the analysis are correct
 * {@linkplain AnalysisConfig#analysisFields()}
 * {@linkplain AnalysisConfig#fields()}
 * {@linkplain AnalysisConfig#byFields()}
 * {@linkplain AnalysisConfig#overFields()}
 * {@linkplain AnalysisConfig#partitionFields()}
 */
public class AnalysisConfigTest
{
    @Rule
    public ExpectedException m_ExpectedException = ExpectedException.none();

    @Test
    public void testFieldConfiguration()
    {
        // Single detector, not pre-summarised
        AnalysisConfig ac = new AnalysisConfig();
        Detector det = new Detector();
        det.setFieldName("responsetime");
        det.setByFieldName("airline");
        det.setPartitionFieldName("sourcetype");
        ac.setDetectors(Arrays.asList(det));

        Set<String> analysisFields = new TreeSet<String>(Arrays.asList(new String [] {
                "responsetime", "airline", "sourcetype"}));

        for (String s : ac.analysisFields())
        {
            Assert.assertTrue(analysisFields.contains(s));
        }

        for (String s : analysisFields)
        {
            Assert.assertTrue(ac.analysisFields().contains(s));
        }

        Assert.assertEquals(1, ac.fields().size());
        Assert.assertTrue(ac.fields().contains("responsetime"));

        Assert.assertEquals(1, ac.byFields().size());
        Assert.assertTrue(ac.byFields().contains("airline"));

        Assert.assertEquals(1, ac.partitionFields().size());
        Assert.assertTrue(ac.partitionFields().contains("sourcetype"));

        Assert.assertNull(ac.getSummaryCountFieldName());

        // Single detector, pre-summarised
        analysisFields.add("summaryCount");
        ac.setSummaryCountFieldName("summaryCount");

        for (String s : ac.analysisFields())
        {
            Assert.assertTrue(analysisFields.contains(s));
        }

        for (String s : analysisFields)
        {
            Assert.assertTrue(ac.analysisFields().contains(s));
        }

        Assert.assertEquals("summaryCount", ac.getSummaryCountFieldName());

        // Multiple detectors, not pre-summarised
        List<Detector> detectors = new ArrayList<>();

        ac = new AnalysisConfig();
        det = new Detector();
        det.setFieldName("metric1");
        det.setByFieldName("by_one");
        det.setPartitionFieldName("partition_one");
        detectors.add(det);

        det = new Detector();
        det.setFieldName("metric2");
        det.setByFieldName("by_two");
        det.setOverFieldName("over_field");
        detectors.add(det);

        det = new Detector();
        det.setFieldName("metric2");
        det.setByFieldName("by_two");
        det.setPartitionFieldName("partition_two");
        detectors.add(det);

        ac.setDetectors(detectors);

        analysisFields = new TreeSet<String>(Arrays.asList(new String [] {
                "metric1", "metric2", "by_one", "by_two", "over_field",
                "partition_one", "partition_two"}));

        for (String s : ac.analysisFields())
        {
            Assert.assertTrue(analysisFields.contains(s));
        }

        for (String s : analysisFields)
        {
            Assert.assertTrue(ac.analysisFields().contains(s));
        }

        Assert.assertEquals(2, ac.fields().size());
        Assert.assertTrue(ac.fields().contains("metric1"));
        Assert.assertTrue(ac.fields().contains("metric2"));

        Assert.assertEquals(2, ac.byFields().size());
        Assert.assertTrue(ac.byFields().contains("by_one"));
        Assert.assertTrue(ac.byFields().contains("by_two"));

        Assert.assertEquals(1, ac.overFields().size());
        Assert.assertTrue(ac.overFields().contains("over_field"));

        Assert.assertEquals(2, ac.partitionFields().size());
        Assert.assertTrue(ac.partitionFields().contains("partition_one"));
        Assert.assertTrue(ac.partitionFields().contains("partition_two"));

        Assert.assertNull(ac.getSummaryCountFieldName());

        // Multiple detectors, pre-summarised
        analysisFields.add("summaryCount");
        ac.setSummaryCountFieldName("summaryCount");

        for (String s : ac.analysisFields())
        {
            Assert.assertTrue(analysisFields.contains(s));
        }

        for (String s : analysisFields)
        {
            Assert.assertTrue(ac.analysisFields().contains(s));
        }

        Assert.assertEquals("summaryCount", ac.getSummaryCountFieldName());
    }

    @Test
    public void testVerify_throws()
    throws JobConfigurationException
    {
        AnalysisConfig ac = new AnalysisConfig();

        // no detector config
        Detector d = new Detector();
        ac.setDetectors(Arrays.asList(new Detector[] {d}));
        try
        {
            ac.verify();
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
            assertEquals(ErrorCodes.INVALID_FIELD_SELECTION, e.getErrorCode());
        }

        // count works with no fields
        d.setFunction("count");
        ac.verify();

        d.setFunction("distinct_count");
        try
        {
            ac.verify();
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
            assertEquals(ErrorCodes.INVALID_FIELD_SELECTION, e.getErrorCode());
        }

        // should work now
        d.setFieldName("somefield");
        d.setOverFieldName("over");
        ac.verify();

        d.setFunction("info_content");
        ac.verify();

        d.setByFieldName("by");
        ac.verify();

        d.setByFieldName(null);
        d.setFunction("made_up_function");
        try
        {
            ac.verify();
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
            assertEquals(ErrorCodes.UNKNOWN_FUNCTION, e.getErrorCode());
        }

        ac.setBatchSpan(-1L);
        try
        {
            ac.verify();
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
            assertEquals(ErrorCodes.INVALID_VALUE, e.getErrorCode());
        }

        ac = new AnalysisConfig();
        ac.setBucketSpan(-1L);
        try
        {
            ac.verify();
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
            assertEquals(ErrorCodes.INVALID_VALUE, e.getErrorCode());
        }

        ac = new AnalysisConfig();
        ac.setPeriod(-1L);
        try
        {
            ac.verify();
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
            assertEquals(ErrorCodes.INVALID_VALUE, e.getErrorCode());
        }

        ac = new AnalysisConfig();
        ac.setLatency(-1L);
        try
        {
            ac.verify();
            Assert.assertTrue(false); // shouldn't get here
        }
        catch (JobConfigurationException e)
        {
            assertEquals(ErrorCodes.INVALID_VALUE, e.getErrorCode());
        }
    }

    @Test
    public void testVerify_GivenNegativeBucketSpan() throws JobConfigurationException
    {
        AnalysisConfig analysisConfig = new AnalysisConfig();
        analysisConfig.setBucketSpan(-1L);
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("BucketSpan cannot be < 0. Value = -1");

        analysisConfig.verify();
    }

    @Test
    public void testVerify_GivenNegativeBatchSpan() throws JobConfigurationException
    {
        AnalysisConfig analysisConfig = new AnalysisConfig();
        analysisConfig.setBatchSpan(-1L);
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("BatchSpan cannot be < 0. Value = -1");

        analysisConfig.verify();
    }

    @Test
    public void testVerify_GivenNegativeLatency() throws JobConfigurationException
    {
        AnalysisConfig analysisConfig = new AnalysisConfig();
        analysisConfig.setLatency(-1L);
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Latency cannot be < 0. Value = -1");

        analysisConfig.verify();
    }

    @Test
    public void testVerify_GivenNegativePeriod() throws JobConfigurationException
    {
        AnalysisConfig analysisConfig = new AnalysisConfig();
        analysisConfig.setPeriod(-1L);
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Period cannot be < 0. Value = -1");

        analysisConfig.verify();
    }

    @Test
    public void testVerify_GivenDefaultConfig_ShouldBeInvalidDueToNoDetectors() throws JobConfigurationException
    {
        AnalysisConfig analysisConfig = new AnalysisConfig();
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("No detectors configured");

        analysisConfig.verify();
    }

    @Test
    public void testVerify_GivenValidConfig() throws JobConfigurationException
    {
        AnalysisConfig analysisConfig = new AnalysisConfig();
        analysisConfig.setBucketSpan(3600L);
        analysisConfig.setBatchSpan(0L);
        analysisConfig.setLatency(0L);
        analysisConfig.setPeriod(0L);
        List<Detector> detectors = new ArrayList<>();
        Detector detector = new Detector();
        detector.setFieldName("count");
        detectors.add(detector);
        analysisConfig.setDetectors(detectors);

        assertTrue(analysisConfig.verify());
    }

    @Test
    public void testHashCode_GivenEqual()
    {
        AnalysisConfig analysisConfig1 = new AnalysisConfig();
        AnalysisConfig analysisConfig2 = new AnalysisConfig();

        assertEquals(analysisConfig1.hashCode(), analysisConfig2.hashCode());
    }
}
