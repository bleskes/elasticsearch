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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class JobConfigurationTest
{
    /**
     * Test the {@link AnalysisConfig#analysisFields()} method which produces
     * a list of analysis fields from the detectors
     */
    @Test
    public void testAnalysisConfigRequiredFields()
    {
        Detector d1 = new Detector();
        d1.setFieldName("field");
        d1.setByFieldName("by");
        d1.setFunction("max");

        Detector d2 = new Detector();
        d2.setFieldName("field2");
        d2.setOverFieldName("over");

        AnalysisConfig ac = new AnalysisConfig();
        ac.setSummaryCountFieldName("agg");
        ac.setDetectors(Arrays.asList(new Detector[] {d1, d2}));

        List<String> analysisFields = ac.analysisFields();
        assertTrue(analysisFields.size() == 5);

        assertTrue(analysisFields.contains("agg"));
        assertTrue(analysisFields.contains("field"));
        assertTrue(analysisFields.contains("by"));
        assertTrue(analysisFields.contains("field2"));
        assertTrue(analysisFields.contains("over"));

        assertFalse(analysisFields.contains("max"));
        assertFalse(analysisFields.contains(""));
        assertFalse(analysisFields.contains(null));

        Detector d3 = new Detector();
        d3.setFunction("count");
        d3.setByFieldName("by2");
        d3.setPartitionFieldName("partition");

        ac = new AnalysisConfig();
        ac.setDetectors(Arrays.asList(new Detector[] {d1, d2, d3}));

        analysisFields = ac.analysisFields();
        assertTrue(analysisFields.size() == 6);

        assertTrue(analysisFields.contains("partition"));
        assertTrue(analysisFields.contains("field"));
        assertTrue(analysisFields.contains("by"));
        assertTrue(analysisFields.contains("by2"));
        assertTrue(analysisFields.contains("field2"));
        assertTrue(analysisFields.contains("over"));

        assertFalse(analysisFields.contains("count"));
        assertFalse(analysisFields.contains("max"));
        assertFalse(analysisFields.contains(""));
        assertFalse(analysisFields.contains(null));
    }

    @Test
    public void testDefaultRenormalizationWindowDays()
    {
        assertNull(new JobConfiguration().getRenormalizationWindowDays());
    }

    @Test
    public void testSetRenormalizationWindowDays()
    {
        JobConfiguration config = new JobConfiguration();
        config.setRenormalizationWindowDays(3L);
        assertEquals(3L, config.getRenormalizationWindowDays().longValue());
    }

    @Test
    public void testSetIgnoreDowntime()
    {
        JobConfiguration config = new JobConfiguration();
        config.setIgnoreDowntime(IgnoreDowntime.ONCE);
        assertEquals(IgnoreDowntime.ONCE, config.getIgnoreDowntime());
    }
}
