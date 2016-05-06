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
package com.prelert.rs.resources;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.prelert.job.JobException;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.messages.Messages;

public class LogsTest
{
    @Rule
    public ExpectedException m_ExpectedException = ExpectedException.none();

    @Before
    public void setup()
    {
        m_ExpectedException.expect(JobException.class);
        m_ExpectedException.expect(
              ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_LOG_FILE_PATH));
    }

    @Test
    public void testJobLogFiles_throwsGivenInvalidJobId()
    throws JobException
    {
        String expectedErrorMessage =
                Messages.getMessage(Messages.LOGFILE_INVALID_CHARS_IN_PATH, "a/job");
        m_ExpectedException.expectMessage(expectedErrorMessage);
        new Logs().jobLogFiles("a/job");
    }

    @Test
    public void testZipLogFiles_throwsGivenInvalidJobId()
    throws JobException
    {
        String expectedErrorMessage =
                Messages.getMessage(Messages.LOGFILE_INVALID_CHARS_IN_PATH, "a\\job");
        m_ExpectedException.expectMessage(expectedErrorMessage);

        new Logs().zipLogFiles("a\\job");
    }

    @Test
    public void testTailDefaultLogFile_throwsGivenInvalidJobId()
    throws JobException
    {
        String expectedErrorMessage =
                Messages.getMessage(Messages.LOGFILE_INVALID_CHARS_IN_PATH, "a\\job");
        m_ExpectedException.expectMessage(expectedErrorMessage);

        new Logs().tailDefaultLogFile("a\\job", 100);
    }

    @Test
    public void testGetLogFile_throwsGivenInvalidJobId()
            throws JobException
    {
        String expectedErrorMessage =
                Messages.getMessage(Messages.LOGFILE_INVALID_CHARS_IN_PATH, "a\\job");
        m_ExpectedException.expectMessage(expectedErrorMessage);

        new Logs().getLogFile("a\\job", "autodetect.log");
    }

    @Test
    public void testGetLogFile_throwsGivenInvalidFileName()
            throws JobException
    {
        String expectedErrorMessage =
                Messages.getMessage(Messages.LOGFILE_INVALID_CHARS_IN_PATH, "aut\\odetect.log");
        m_ExpectedException.expectMessage(expectedErrorMessage);

        new Logs().getLogFile("airplane", "aut\\odetect.log");
    }

    @Test
    public void testTailLogFile_throwsGivenInvalidJobId()
            throws JobException
    {
        String expectedErrorMessage =
                Messages.getMessage(Messages.LOGFILE_INVALID_CHARS_IN_PATH, "a/job");
        m_ExpectedException.expectMessage(expectedErrorMessage);

        new Logs().tailLogFile("a/job", "autodetect.log", 100);
    }

    @Test
    public void testTailLogFile_throwsGivenInvalidFileName()
            throws JobException
    {
        String expectedErrorMessage =
                Messages.getMessage(Messages.LOGFILE_INVALID_CHARS_IN_PATH, "aut\\odetect.log");
        m_ExpectedException.expectMessage(expectedErrorMessage);

        new Logs().tailLogFile("airplane", "aut\\odetect.log", 100);
    }
}
