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

import org.junit.Test;

import com.prelert.job.errorcodes.ErrorCodes;

public class ApiErrorTest
{
    @Test
    public void testToJson_GivenEmptyApiError()
    {
        ApiError apiError = new ApiError();

        assertEquals("{\n}\n", apiError.toJson());
    }

    @Test
    public void testToJson_GivenMessageAndCause()
    {
        ApiError apiError = new ApiError();
        apiError.setErrorCode(ErrorCodes.DATA_ERROR);
        apiError.setMessage("Some message");
        apiError.setCause(new RuntimeException("Cause message").toString());

        StringBuilder expected = new StringBuilder();
        expected.append("{\n")
                .append("  \"message\" : \"Some message\",\n")
                .append("  \"errorCode\" : 30001,\n")
                .append("  \"cause\" : \"java.lang.RuntimeException: Cause message\"\n")
                .append("}\n");

        assertEquals(expected.toString(), apiError.toJson());
    }

    @Test
    public void testToJson_GivenMessageCauseAndHost()
    {
        ApiError apiError = new ApiError(ErrorCodes.DATA_ERROR);
        apiError.setMessage("Some message");
        apiError.setCause(new RuntimeException("Cause message").toString());
        apiError.setHost("cray-1");

        StringBuilder expected = new StringBuilder();
        expected.append("{\n")
                .append("  \"message\" : \"Some message\",\n")
                .append("  \"errorCode\" : 30001,\n")
                .append("  \"cause\" : \"java.lang.RuntimeException: Cause message\",\n")
                .append("  \"hostname\" : \"cray-1\"\n")
                .append("}\n");

        assertEquals(expected.toString(), apiError.toJson());
    }
}
