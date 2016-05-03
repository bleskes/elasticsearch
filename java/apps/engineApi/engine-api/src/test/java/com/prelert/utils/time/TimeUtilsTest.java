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

package com.prelert.utils.time;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TimeUtilsTest
{
    @Test
    public void testParseIso8601AsEpochMillis()
    {
        assertEquals(1462096800000L, TimeUtils.parseIso8601AsEpochMillis("2016-05-01T10:00:00Z"));
        assertEquals(1462096800333L, TimeUtils.parseIso8601AsEpochMillis("2016-05-01T10:00:00.333Z"));
        assertEquals(1462096800334L, TimeUtils.parseIso8601AsEpochMillis("2016-05-01T10:00:00.334+00"));
        assertEquals(1462096800335L, TimeUtils.parseIso8601AsEpochMillis("2016-05-01T10:00:00.335+0000"));
        assertEquals(1462096800333L, TimeUtils.parseIso8601AsEpochMillis("2016-05-01T10:00:00.333+00:00"));
        assertEquals(1462093200333L, TimeUtils.parseIso8601AsEpochMillis("2016-05-01T10:00:00.333+01"));
        assertEquals(1462093200333L, TimeUtils.parseIso8601AsEpochMillis("2016-05-01T10:00:00.333+0100"));
        assertEquals(1462093200333L, TimeUtils.parseIso8601AsEpochMillis("2016-05-01T10:00:00.333+01:00"));
        assertEquals(1462098600333L, TimeUtils.parseIso8601AsEpochMillis("2016-05-01T10:00:00.333-00:30"));
        assertEquals(1462098600333L, TimeUtils.parseIso8601AsEpochMillis("2016-05-01T10:00:00.333-0030"));
    }
}
