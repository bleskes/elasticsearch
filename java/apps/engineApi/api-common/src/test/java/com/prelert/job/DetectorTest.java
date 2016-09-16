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

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

import com.prelert.job.detectionrules.Connective;
import com.prelert.job.detectionrules.DetectionRule;
import com.prelert.job.detectionrules.RuleCondition;

public class DetectorTest
{
    @Test
    public void testEquals_GivenEqual()
    {
        Detector detector1 = new Detector();
        detector1.setDetectorDescription("foo");
        detector1.setFunction("mean");
        detector1.setFieldName("field");
        detector1.setByFieldName("by");
        detector1.setOverFieldName("over");
        detector1.setPartitionFieldName("partition");
        detector1.setUseNull(false);

        Detector detector2 = new Detector();
        detector2.setDetectorDescription("foo");
        detector2.setFunction("mean");
        detector2.setFieldName("field");
        detector2.setByFieldName("by");
        detector2.setOverFieldName("over");
        detector2.setPartitionFieldName("partition");
        detector2.setUseNull(false);

        assertTrue(detector1.equals(detector2));
        assertTrue(detector2.equals(detector1));
        assertEquals(detector1.hashCode(), detector2.hashCode());
    }

    @Test
    public void testEquals_GivenDifferentDetectorDescription()
    {
        Detector detector1 = createDetector();
        Detector detector2 = createDetector();
        detector2.setDetectorDescription("bar");

        assertFalse(detector1.equals(detector2));
    }

    @Test
    public void testEquals_GivenDifferentByFieldName()
    {
        Detector detector1 = createDetector();
        Detector detector2 = createDetector();

        assertEquals(detector1, detector2);

        detector2.setByFieldName("aa");
        assertFalse(detector1.equals(detector2));
    }

    @Test
    public void testEquals_GivenDifferentRules()
    {
        Detector detector1 = createDetector();
        Detector detector2 = createDetector();
        detector2.getDetectorRules().get(0).setConditionsConnective(Connective.AND);

        assertFalse(detector1.equals(detector2));
        assertFalse(detector2.equals(detector1));
    }

    @Test
    public void testExtractAnalysisFields()
    {
        Detector detector = createDetector();
        assertEquals(Arrays.asList("by", "over", "partition"), detector.extractAnalysisFields());
        detector.setPartitionFieldName(null);
        assertEquals(Arrays.asList("by", "over"), detector.extractAnalysisFields());
        detector.setByFieldName(null);
        assertEquals(Arrays.asList("over"), detector.extractAnalysisFields());
        detector.setOverFieldName(null);
        assertTrue(detector.extractAnalysisFields().isEmpty());
    }

    @Test
    public void testExtractReferencedLists()
    {
        Detector detector = createDetector();
        detector.setDetectorRules(Arrays.asList(new DetectionRule(), new DetectionRule()));
        detector.getDetectorRules().get(0).setRuleConditions(Arrays.asList(
                RuleCondition.createCategorical("foo", "list1")));
        detector.getDetectorRules().get(1).setRuleConditions(Arrays.asList(
                RuleCondition.createCategorical("bar", "list2")));

        assertEquals(new HashSet<>(Arrays.asList("list1", "list2")), detector.extractReferencedLists());
    }

    private Detector createDetector()
    {
        Detector detector = new Detector();
        detector.setFunction("mean");
        detector.setFieldName("field");
        detector.setByFieldName("by");
        detector.setOverFieldName("over");
        detector.setPartitionFieldName("partition");
        detector.setUseNull(true);

        DetectionRule rule = new DetectionRule();
        detector.setDetectorRules(Arrays.asList(rule));

        return detector;
    }
}
