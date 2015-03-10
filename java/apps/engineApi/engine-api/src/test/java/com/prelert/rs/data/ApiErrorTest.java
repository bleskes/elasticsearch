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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ApiErrorTest
{
    @Test
    public void testToJson_GivenEmptyApiError()
    {
        ApiError apiError = new ApiError();

        StringBuilder expected = new StringBuilder();
        expected.append("{").append(System.lineSeparator())
                .append("}").append(System.lineSeparator());

        assertEquals(expected.toString(), apiError.toJson());
    }

    @Test
    public void testToJson_GivenFullyPopulatedApiError()
    {
        ApiError apiError = new ApiError();
        apiError.setErrorCode(ErrorCode.DATA_ERROR);
        apiError.setMessage("Some message");
        apiError.setCause(new RuntimeException("Cause message"));

        StringBuilder expected = new StringBuilder();
        expected.append("{")
                .append(System.lineSeparator())
                .append("  \"message\" : \"Some message\",")
                .append(System.lineSeparator())
                .append("  \"errorCode\" : 30001,")
                .append(System.lineSeparator())
                .append("  \"cause\" : \"java.lang.RuntimeException: Cause message\"")
                .append(System.lineSeparator())
                .append("}")
                .append(System.lineSeparator());

        assertEquals(expected.toString(), apiError.toJson());
    }
}
