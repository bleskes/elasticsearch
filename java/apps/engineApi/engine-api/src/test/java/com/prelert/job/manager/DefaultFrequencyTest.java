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

package com.prelert.job.manager;

import static org.junit.Assert.assertEquals;

import java.time.Duration;

import org.junit.Test;

public class DefaultFrequencyTest
{
    @Test (expected = IllegalArgumentException.class)
    public void testCalc_GivenNegative()
    {
        DefaultFrequency.ofBucketSpan(-1);
    }

    @Test
    public void testCalc()
    {
        assertEquals(Duration.ofMinutes(1), DefaultFrequency.ofBucketSpan(1));
        assertEquals(Duration.ofMinutes(1), DefaultFrequency.ofBucketSpan(30));
        assertEquals(Duration.ofMinutes(1), DefaultFrequency.ofBucketSpan(60));
        assertEquals(Duration.ofMinutes(1), DefaultFrequency.ofBucketSpan(90));
        assertEquals(Duration.ofMinutes(1), DefaultFrequency.ofBucketSpan(120));
        assertEquals(Duration.ofMinutes(1), DefaultFrequency.ofBucketSpan(121));

        assertEquals(Duration.ofSeconds(61), DefaultFrequency.ofBucketSpan(122));
        assertEquals(Duration.ofSeconds(75), DefaultFrequency.ofBucketSpan(150));
        assertEquals(Duration.ofSeconds(150), DefaultFrequency.ofBucketSpan(300));
        assertEquals(Duration.ofMinutes(10), DefaultFrequency.ofBucketSpan(1200));

        assertEquals(Duration.ofMinutes(10), DefaultFrequency.ofBucketSpan(1201));
        assertEquals(Duration.ofMinutes(10), DefaultFrequency.ofBucketSpan(1800));
        assertEquals(Duration.ofMinutes(10), DefaultFrequency.ofBucketSpan(3600));
        assertEquals(Duration.ofMinutes(10), DefaultFrequency.ofBucketSpan(7200));
        assertEquals(Duration.ofMinutes(10), DefaultFrequency.ofBucketSpan(12 * 3600));

        assertEquals(Duration.ofHours(1), DefaultFrequency.ofBucketSpan(12 * 3600 + 1));
        assertEquals(Duration.ofHours(1), DefaultFrequency.ofBucketSpan(13 * 3600));
        assertEquals(Duration.ofHours(1), DefaultFrequency.ofBucketSpan(24 * 3600));
        assertEquals(Duration.ofHours(1), DefaultFrequency.ofBucketSpan(48 * 3600));
    }
}
