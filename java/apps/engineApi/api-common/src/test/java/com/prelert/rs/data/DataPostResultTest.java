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
package com.prelert.rs.data;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Test;

import com.prelert.job.DataCounts;
import com.prelert.job.errorcodes.ErrorCodes;

public class DataPostResultTest
{
    @Test
    public void testEqualsAndHash()
    {
        DataCounts dc = createCounts(20, 30, 40, 10, 5, 15, 25, 35, 45, 50);
        DataPostResponse dp = new DataPostResponse("foo", dc);

        assertEquals(dp, new DataPostResponse("foo", dc));
        assertEquals(dp.hashCode(), new DataPostResponse("foo", dc).hashCode());

        assertFalse(dp.equals(new DataPostResponse("bar", dc)));

        ApiError error = new ApiError(ErrorCodes.UNCOMPRESSED_DATA);
        DataPostResponse errorDp = new DataPostResponse("foo", error);

        assertFalse(dp.equals(errorDp));
        errorDp.setJobId("error");
        assertEquals(errorDp, new DataPostResponse("error", error));
        assertEquals(errorDp.hashCode(), new DataPostResponse("error", error).hashCode());
    }


    private static DataCounts createCounts(long bucketCount,
            long processedRecordCount, long processedFieldCount,
            long inputBytes, long inputFieldCount,
            long invalidDateCount, long missingFieldCount,
            long outOfOrderTimeStampCount, long failedTransformCount, long latestRecordTime)
    {
        DataCounts counts = new DataCounts();
        counts.setBucketCount(bucketCount);
        counts.setProcessedRecordCount(processedRecordCount);
        counts.setProcessedFieldCount(processedFieldCount);
        counts.setInputBytes(inputBytes);
        counts.setInputFieldCount(inputFieldCount);
        counts.setInvalidDateCount(invalidDateCount);
        counts.setMissingFieldCount(missingFieldCount);
        counts.setOutOfOrderTimeStampCount(outOfOrderTimeStampCount);
        counts.setFailedTransformCount(failedTransformCount);
        counts.setLatestRecordTimeStamp(new Date(latestRecordTime));
        return counts;
    }

}
