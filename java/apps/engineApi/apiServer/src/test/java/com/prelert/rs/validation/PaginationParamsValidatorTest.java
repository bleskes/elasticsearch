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

package com.prelert.rs.validation;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.rs.exception.InvalidParametersException;

public class PaginationParamsValidatorTest
{
    @Rule public ExpectedException m_ExpectedException = ExpectedException.none();

    @Test
    public void testValidate_GivenSkipIsMinusOne()
    {
        m_ExpectedException.expect(InvalidParametersException.class);
        m_ExpectedException.expectMessage("Parameter 'skip' cannot be < 0");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_SKIP_PARAM));

        new PaginationParamsValidator(-1, 100).validate();
    }

    @Test
    public void testValidate_GivenSkipIsMinusTen()
    {
        m_ExpectedException.expect(InvalidParametersException.class);
        m_ExpectedException.expectMessage("Parameter 'skip' cannot be < 0");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_SKIP_PARAM));

        new PaginationParamsValidator(-10, 100).validate();
    }

    @Test
    public void testValidate_GivenTakeIsMinusOne()
    {
        m_ExpectedException.expect(InvalidParametersException.class);
        m_ExpectedException.expectMessage("Parameter 'take' cannot be < 0");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_TAKE_PARAM));

        new PaginationParamsValidator(0, -1).validate();
    }

    @Test
    public void testValidate_GivenTakeIsMinusHundred()
    {
        m_ExpectedException.expect(InvalidParametersException.class);
        m_ExpectedException.expectMessage("Parameter 'take' cannot be < 0");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_TAKE_PARAM));

        new PaginationParamsValidator(0, -100).validate();
    }

    @Test
    public void testValidate_GivenSkipAndTakeAreValid()
    {
        new PaginationParamsValidator(0, 0).validate();
        new PaginationParamsValidator(100, 0).validate();
        new PaginationParamsValidator(0, 100).validate();
        new PaginationParamsValidator(50, 500).validate();
    }
}
