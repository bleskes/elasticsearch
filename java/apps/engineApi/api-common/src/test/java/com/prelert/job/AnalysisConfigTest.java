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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.Assert;

import org.junit.Test;

public class AnalysisConfigTest
{
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

        Set<String> termFields = new TreeSet<String>(Arrays.asList(new String [] {
                "airline", "sourcetype"}));
        Set<String> analysisFields = new TreeSet<String>(Arrays.asList(new String [] {
                "responsetime", "airline", "sourcetype"}));

        Assert.assertEquals(termFields.size(), ac.termFields().size());
        Assert.assertEquals(analysisFields.size(), ac.analysisFields().size());

        for (String s : ac.termFields())
        {
            Assert.assertTrue(termFields.contains(s));
        }

        for (String s : termFields)
        {
            Assert.assertTrue(ac.termFields().contains(s));
        }

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
        ac.setInfluencers(Arrays.asList("Influencer_Field"));
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

        termFields = new TreeSet<String>(Arrays.asList(new String [] {
                "by_one", "by_two", "over_field",
                "partition_one", "partition_two", "Influencer_Field"}));
        analysisFields = new TreeSet<String>(Arrays.asList(new String [] {
                "metric1", "metric2", "by_one", "by_two", "over_field",
                "partition_one", "partition_two", "Influencer_Field"}));

        Assert.assertEquals(termFields.size(), ac.termFields().size());
        Assert.assertEquals(analysisFields.size(), ac.analysisFields().size());

        for (String s : ac.termFields())
        {
            Assert.assertTrue(termFields.contains(s));
        }

        for (String s : termFields)
        {
            Assert.assertTrue(ac.termFields().contains(s));
        }

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
    public void testEquals_GivenSameReference()
    {
        AnalysisConfig config = new AnalysisConfig();
        assertTrue(config.equals(config));
    }

    @Test
    public void testEquals_GivenDifferentClass()
    {
        assertFalse(new AnalysisConfig().equals("a string"));
    }

    @Test
    public void testEquals_GivenNull()
    {
        assertFalse(new AnalysisConfig().equals(null));
    }

    @Test
    public void testEquals_GivenEqualConfig()
    {
        AnalysisConfig config1 = new AnalysisConfig();
        config1.setBatchSpan(86400L);
        config1.setBucketSpan(3600L);
        config1.setCategorizationFieldName("cat");
        Detector detector1 = new Detector();
        detector1.setFunction("count");
        config1.setDetectors(Arrays.asList(detector1));
        config1.setInfluencers(Arrays.asList("myInfluencer"));
        config1.setLatency(3600L);
        config1.setPeriod(100L);
        config1.setSummaryCountFieldName("sumCount");

        AnalysisConfig config2 = new AnalysisConfig();
        config2.setBatchSpan(86400L);
        config2.setBucketSpan(3600L);
        config2.setCategorizationFieldName("cat");
        Detector detector2 = new Detector();
        detector2.setFunction("count");
        config2.setDetectors(Arrays.asList(detector2));
        config2.setInfluencers(Arrays.asList("myInfluencer"));
        config2.setLatency(3600L);
        config2.setPeriod(100L);
        config2.setSummaryCountFieldName("sumCount");

        assertTrue(config1.equals(config2));
        assertTrue(config2.equals(config1));
        assertEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    public void testEquals_GivenDifferentBatchSpan()
    {
        AnalysisConfig config1 = new AnalysisConfig();
        config1.setBatchSpan(86400L);

        AnalysisConfig config2 = new AnalysisConfig();
        config2.setBatchSpan(0L);

        assertFalse(config1.equals(config2));
        assertFalse(config2.equals(config1));
    }

    @Test
    public void testEquals_GivenDifferentBucketSpan()
    {
        AnalysisConfig config1 = new AnalysisConfig();
        config1.setBucketSpan(1800L);

        AnalysisConfig config2 = new AnalysisConfig();
        config2.setBucketSpan(3600L);

        assertFalse(config1.equals(config2));
        assertFalse(config2.equals(config1));
    }

    @Test
    public void testEquals_GivenCategorizationField()
    {
        AnalysisConfig config1 = new AnalysisConfig();
        config1.setCategorizationFieldName("foo");

        AnalysisConfig config2 = new AnalysisConfig();
        config2.setCategorizationFieldName("bar");

        assertFalse(config1.equals(config2));
        assertFalse(config2.equals(config1));
    }

    @Test
    public void testEquals_GivenDifferentDetector()
    {
        AnalysisConfig config1 = new AnalysisConfig();
        Detector detector1 = new Detector();
        detector1.setFunction("low_count");
        config1.setDetectors(Arrays.asList(detector1));

        AnalysisConfig config2 = new AnalysisConfig();
        Detector detector2 = new Detector();
        detector2.setFunction("high_count");
        config2.setDetectors(Arrays.asList(detector2));

        assertFalse(config1.equals(config2));
        assertFalse(config2.equals(config1));
    }

    @Test
    public void testEquals_GivenDifferentInfluencers()
    {
        AnalysisConfig config1 = new AnalysisConfig();
        config1.setInfluencers(Arrays.asList("foo"));

        AnalysisConfig config2 = new AnalysisConfig();
        config2.setInfluencers(Arrays.asList("bar"));

        assertFalse(config1.equals(config2));
        assertFalse(config2.equals(config1));
    }

    @Test
    public void testEquals_GivenDifferentLatency()
    {
        AnalysisConfig config1 = new AnalysisConfig();
        config1.setLatency(1800L);

        AnalysisConfig config2 = new AnalysisConfig();
        config2.setLatency(3600L);

        assertFalse(config1.equals(config2));
        assertFalse(config2.equals(config1));
    }

    @Test
    public void testEquals_GivenDifferentPeriod()
    {
        AnalysisConfig config1 = new AnalysisConfig();
        config1.setPeriod(1800L);

        AnalysisConfig config2 = new AnalysisConfig();
        config2.setPeriod(3600L);

        assertFalse(config1.equals(config2));
        assertFalse(config2.equals(config1));
    }

    @Test
    public void testEquals_GivenSummaryCountField()
    {
        AnalysisConfig config1 = new AnalysisConfig();
        config1.setSummaryCountFieldName("foo");

        AnalysisConfig config2 = new AnalysisConfig();
        config2.setSummaryCountFieldName("bar");

        assertFalse(config1.equals(config2));
        assertFalse(config2.equals(config1));
    }

    @Test
    public void testEquals_GivenMultivariateByField()
    {
        AnalysisConfig config1 = new AnalysisConfig();
        config1.setMultivariateByFields(true);

        AnalysisConfig config2 = new AnalysisConfig();
        config2.setMultivariateByFields(false);

        assertFalse(config1.equals(config2));
        assertFalse(config2.equals(config1));
    }
}
