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

import static org.junit.Assert.*;

import org.junit.Test;

import com.prelert.job.JobException;
import com.prelert.job.UnknownJobException;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.rs.data.ApiError;

public class MapperUtilsTest {

    @Test
    public void testApiErrorFromJobException()
    {
        Exception cause = new ArrayIndexOutOfBoundsException();
        String causeString = cause.toString();

        JobException e = new UnknownJobException("job", "error message",
                                                ErrorCodes.BUCKET_RESET_NOT_SUPPORTED,
                                                cause);

        ApiError error = MapperUtils.apiErrorFromJobException(e);
        assertEquals(ErrorCodes.BUCKET_RESET_NOT_SUPPORTED, error.getErrorCode());
        assertEquals("error message", error.getMessage());
        assertEquals(causeString, error.getCause());
    }

}
