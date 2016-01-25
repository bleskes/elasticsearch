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

package com.prelert.job.config;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.prelert.job.Detector;

public class DefaultDetectorNameTest
{
    @Test
    public void testOf_GivenEmptyDetector()
    {
        Detector detector = new Detector();

        assertEquals("", DefaultDetectorName.of(detector));
    }

    @Test
    public void testOf_GivenOnlyFieldName()
    {
        Detector detector = new Detector();
        detector.setFieldName("value");

        assertEquals("value", DefaultDetectorName.of(detector));
    }

    @Test
    public void testOf_GivenOnlyFunctionAndFieldName()
    {
        Detector detector = new Detector();
        detector.setFunction("min");
        detector.setFieldName("value");

        assertEquals("min(value)", DefaultDetectorName.of(detector));
    }

    @Test
    public void testOf_GivenOnlyFunctionAndFieldNameWithNonWordChars()
    {
        Detector detector = new Detector();
        detector.setFunction("min");
        detector.setFieldName("val-ue");

        assertEquals("min(\"val-ue\")", DefaultDetectorName.of(detector));
    }

    @Test
    public void testOf_GivenFullyPopulatedDetector()
    {
        Detector detector = new Detector();
        detector.setFunction("sum");
        detector.setFieldName("value");
        detector.setByFieldName("airline");
        detector.setOverFieldName("region");
        detector.setUseNull(true);
        detector.setPartitionFieldName("planet");
        detector.setExcludeFrequent("true");

        assertEquals("sum(value) by airline over region usenull=true partitionfield=planet excludefrequent=true",
                DefaultDetectorName.of(detector));
    }
}
