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
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;

import com.prelert.job.JobDetails.Counts;

public class JobDetailsTest
{

    @Test
    public void testHashCode_GivenEqualJobDetails()
    {
        JobConfiguration jobConfiguration = new JobConfiguration();
        JobDetails jobDetails1 = new JobDetails("foo", jobConfiguration);
        JobDetails jobDetails2 = new JobDetails("foo", jobConfiguration);
        Date createTime = new Date();
        jobDetails1.setCreateTime(createTime);
        jobDetails2.setCreateTime(createTime);

        assertEquals(jobDetails1.hashCode(), jobDetails2.hashCode());
    }

    @Test
    public void testCountsEquals_GivenEqualCounts()
    {
        Counts counts1 = createCounts(1, 2, 3, 4, 5, 6, 7, 8, 9);
        Counts counts2 = createCounts(1, 2, 3, 4, 5, 6, 7, 8, 9);

        assertTrue(counts1.equals(counts2));
        assertTrue(counts2.equals(counts1));
    }

    @Test
    public void testCountsHashCode_GivenEqualCounts()
    {
        Counts counts1 = createCounts(1, 2, 3, 4, 5, 6, 7, 8, 9);
        Counts counts2 = createCounts(1, 2, 3, 4, 5, 6, 7, 8, 9);

        assertEquals(counts1.hashCode(), counts2.hashCode());
    }

    private static Counts createCounts(long bucketCount,
            long processedRecordCount, long processedFieldCount,
            long inputBytes, long inputFieldCount, long inputRecordCount,
            long invalidDateCount, long missingFieldCount,
            long outOfOrderTimeStampCount)
    {
        Counts counts = new Counts();
        counts.setBucketCount(bucketCount);
        counts.setProcessedRecordCount(processedRecordCount);
        counts.setProcessedFieldCount(processedFieldCount);
        counts.setInputBytes(inputBytes);
        counts.setInputFieldCount(inputFieldCount);
        counts.setInputRecordCount(inputRecordCount);
        counts.setInvalidDateCount(invalidDateCount);
        counts.setMissingFieldCount(missingFieldCount);
        counts.setOutOfOrderTimeStampCount(outOfOrderTimeStampCount);
        return counts;
    }
}
