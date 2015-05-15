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

package com.prelert.rs.provider;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.Response;

import org.junit.Test;

import com.prelert.job.DataCounts;
import com.prelert.job.process.exceptions.DataUploadException;

public class DataUploadExceptionMapperTest
{
    @Test
    public void testToResponse()
    {
        DataCounts dataCounts = new DataCounts();
        dataCounts.setProcessedRecordCount(3000);
        dataCounts.setInvalidDateCount(3);
        dataCounts.setMissingFieldCount(5);
        dataCounts.setOutOfOrderTimeStampCount(1);
        DataUploadException dataUploadException = new DataUploadException(dataCounts,
                new IllegalArgumentException("foo"));

        Response response = new DataUploadExceptionMapper().toResponse(dataUploadException);

        String expected = "";
        expected += "{\n";
        expected += "  \"message\" : \"An error occurred after processing 3004 records. "
                + "(invalidDateCount = 3, missingFieldCount = 5, outOfOrderTimeStampCount = 1)\",\n";
        expected += "  \"errorCode\" : 30001,\n";
        expected += "  \"cause\" : \"java.lang.IllegalArgumentException: foo\"\n";
        expected += "}\n";
        assertEquals(expected, response.getEntity());
    }
}
