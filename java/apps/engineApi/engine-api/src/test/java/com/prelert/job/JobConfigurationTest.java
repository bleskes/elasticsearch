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


import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

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
        Assert.assertTrue(analysisFields.size() == 5);

        Assert.assertTrue(analysisFields.contains("agg"));
        Assert.assertTrue(analysisFields.contains("field"));
        Assert.assertTrue(analysisFields.contains("by"));
        Assert.assertTrue(analysisFields.contains("field2"));
        Assert.assertTrue(analysisFields.contains("over"));

        Assert.assertFalse(analysisFields.contains("max"));
        Assert.assertFalse(analysisFields.contains(""));
        Assert.assertFalse(analysisFields.contains(null));

        Detector d3 = new Detector();
        d3.setFunction("count");
        d3.setByFieldName("by2");
        d3.setPartitionFieldName("partition");

        ac = new AnalysisConfig();
        ac.setDetectors(Arrays.asList(new Detector[] {d1, d2, d3}));

        analysisFields = ac.analysisFields();
        Assert.assertTrue(analysisFields.size() == 6);

        Assert.assertTrue(analysisFields.contains("partition"));
        Assert.assertTrue(analysisFields.contains("field"));
        Assert.assertTrue(analysisFields.contains("by"));
        Assert.assertTrue(analysisFields.contains("by2"));
        Assert.assertTrue(analysisFields.contains("field2"));
        Assert.assertTrue(analysisFields.contains("over"));

        Assert.assertFalse(analysisFields.contains("count"));
        Assert.assertFalse(analysisFields.contains("max"));
        Assert.assertFalse(analysisFields.contains(""));
        Assert.assertFalse(analysisFields.contains(null));
    }
}
