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

package com.prelert.job;

import org.hamcrest.Description;
import org.junit.internal.matchers.TypeSafeMatcher;

import com.prelert.rs.data.ErrorCode;

/**
 * Matcher class for job exceptions.
 * Checks the expected error code is returned
 * @param <E>
 */
public class ErrorCodeMatcher <E extends JobException> extends TypeSafeMatcher<E>
{
    private ErrorCode m_ExpectedErrorCode;
    private ErrorCode m_ActualErrorCode;

    public static <E extends JobException> ErrorCodeMatcher<E> hasErrorCode(ErrorCode expected)
    {
        return new ErrorCodeMatcher<E>(expected);
    }

    private ErrorCodeMatcher(ErrorCode expectedErrorCode)
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
    public boolean matchesSafely(E item)
    {
        m_ActualErrorCode = item.getErrorCode();
        return m_ActualErrorCode.equals(m_ExpectedErrorCode);
    }

}