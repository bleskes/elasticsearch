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

package com.prelert.job.process.params;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.prelert.job.process.params.DataLoadParams;

public class DataLoadParamsTest
{
    @Test
    public void testGetStart()
    {
        assertEquals("", new DataLoadParams(false, new TimeRange(null, null)).getStart());
        assertEquals("3", new DataLoadParams(false, new TimeRange(3L, null)).getStart());
    }

    @Test
    public void testGetEnd()
    {
        assertEquals("", new DataLoadParams(false, new TimeRange(null, null)).getEnd());
        assertEquals("1", new DataLoadParams(false, new TimeRange(null, 1L)).getEnd());
    }

    @Test
    public void testIsResettingBuckets()
    {
        assertFalse(new DataLoadParams(false, new TimeRange(null, null)).isResettingBuckets());
        assertTrue(new DataLoadParams(false, new TimeRange(5L, null)).isResettingBuckets());
    }

    @Test
    public void testIsPersisting()
    {
        assertFalse(new DataLoadParams(false, new TimeRange(null, null)).isPersisting());
        assertTrue(new DataLoadParams(true, new TimeRange(null, null)).isPersisting());
    }

}
