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
package com.prelert.job.detectionrules.verification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.prelert.job.Detector;

public class ScopingLevelTest
{
    @Test
    public void testIsHigherThan()
    {
        assertTrue(ScopingLevel.PARTITION.isHigherThan(ScopingLevel.OVER));
        assertTrue(ScopingLevel.PARTITION.isHigherThan(ScopingLevel.BY));
        assertTrue(ScopingLevel.OVER.isHigherThan(ScopingLevel.BY));

        assertFalse(ScopingLevel.PARTITION.isHigherThan(ScopingLevel.PARTITION));
        assertFalse(ScopingLevel.OVER.isHigherThan(ScopingLevel.PARTITION));
        assertFalse(ScopingLevel.OVER.isHigherThan(ScopingLevel.OVER));
        assertFalse(ScopingLevel.BY.isHigherThan(ScopingLevel.PARTITION));
        assertFalse(ScopingLevel.BY.isHigherThan(ScopingLevel.OVER));
        assertFalse(ScopingLevel.BY.isHigherThan(ScopingLevel.BY));
    }

    @Test
    public void testFrom()
    {
        Detector detector = new Detector();
        detector.setPartitionFieldName("myPartition");
        detector.setOverFieldName("myOver");
        detector.setByFieldName("myBy");

        assertEquals(ScopingLevel.PARTITION, ScopingLevel.from(detector, "myPartition"));
        assertEquals(ScopingLevel.OVER, ScopingLevel.from(detector, "myOver"));
        assertEquals(ScopingLevel.BY, ScopingLevel.from(detector, "myBy"));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testFrom_GivenNonAnalysisField()
    {
        Detector detector = new Detector();
        detector.setPartitionFieldName("myPartition");
        detector.setOverFieldName("myOver");
        detector.setByFieldName("myBy");

        ScopingLevel.from(detector, "none");
    }
}
