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

package com.prelert.job.errorcodes;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class ErrorCodeMatcher extends TypeSafeMatcher<HasErrorCode> {

    private ErrorCodes m_ExpectedErrorCode;
    private ErrorCodes m_ActualErrorCode;

    public static ErrorCodeMatcher hasErrorCode(ErrorCodes expected)
    {
        return new ErrorCodeMatcher(expected);
    }

    private ErrorCodeMatcher(ErrorCodes expectedErrorCode)
    {
        m_ExpectedErrorCode = expectedErrorCode;
    }

    @Override
    public void describeTo(Description description)
    {
        description.appendValue(m_ActualErrorCode)
                .appendText(" was found instead of ")
                .appendValue(m_ExpectedErrorCode);
    }

    @Override
    public boolean matchesSafely(HasErrorCode item)
    {
        m_ActualErrorCode = item.getErrorCode();
        return m_ActualErrorCode.equals(m_ExpectedErrorCode);
    }

}