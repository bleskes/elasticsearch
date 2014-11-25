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

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class DetectorTest
{
    @Test
    public void testHashCode_GivenEqual()
    {
        Map<String, Object> detectorMap = new HashMap<>();
        detectorMap.put(Detector.FUNCTION, "mean");
        detectorMap.put(Detector.FIELD_NAME, "field");
        detectorMap.put(Detector.BY_FIELD_NAME, "by");
        detectorMap.put(Detector.OVER_FIELD_NAME, "over");
        detectorMap.put(Detector.PARTITION_FIELD_NAME, "partition");
        detectorMap.put(Detector.USE_NULL, false);
        Detector detector1 = new Detector(detectorMap);
        Detector detector2 = new Detector(detectorMap);

        assertEquals(detector1.hashCode(), detector2.hashCode());
    }
}
