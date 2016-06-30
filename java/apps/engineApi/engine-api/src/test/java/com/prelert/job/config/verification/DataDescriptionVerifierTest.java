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
package com.prelert.job.config.verification;

import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.prelert.job.DataDescription;

public class DataDescriptionVerifierTest
{
    @Rule
    public ExpectedException m_ExpectedException = ExpectedException.none();

    @Test
    public void testVerify_GivenNullTimeFormat() throws JobConfigurationException
    {
        DataDescription description = new DataDescription();
        description.setTimeFormat(null);

        assertTrue(DataDescriptionVerifier.verify(description));
    }

    @Test
    public void testVerify_GivenEmptyTimeFormat() throws JobConfigurationException
    {
        DataDescription description = new DataDescription();
        description.setTimeFormat("");

        assertTrue(DataDescriptionVerifier.verify(description));
    }

    @Test
    public void testVerify_GivenTimeFormatIsEpoch() throws JobConfigurationException
    {
        DataDescription description = new DataDescription();
        description.setTimeFormat("epoch");

        assertTrue(DataDescriptionVerifier.verify(description));
    }

    @Test
    public void testVerify_GivenTimeFormatIsEpochMs() throws JobConfigurationException
    {
        DataDescription description = new DataDescription();
        description.setTimeFormat("epoch_ms");

        assertTrue(DataDescriptionVerifier.verify(description));
    }

    @Test
    public void testVerify_GivenTimeFormatIsValidDateFormat() throws JobConfigurationException
    {
        DataDescription description = new DataDescription();
        description.setTimeFormat("yyyy-MM-dd HH");

        assertTrue(DataDescriptionVerifier.verify(description));
    }

    @Test
    public void testVerify_GivenTimeFormatIsInvalidDateFormat() throws JobConfigurationException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid Time format string 'invalid'");

        DataDescription description = new DataDescription();
        description.setTimeFormat("invalid");

        DataDescriptionVerifier.verify(description);
    }

    @Test
    public void testVerify_GivenTimeFormatIsValidButDoesNotContainTime() throws JobConfigurationException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid Time format string 'y-M-dd'");

        DataDescription description = new DataDescription();
        description.setTimeFormat("y-M-dd");

        DataDescriptionVerifier.verify(description);
    }

    @Test
    public void testVerify_GivenTimeFormatIsInvalidDateParseFormat()
    throws JobConfigurationException
    {
        String badFormat = "YYY-mm-UU hh:mm:ssY";
        DataDescription dd = new DataDescription();

        dd.setTimeFormat(badFormat);
        try
        {
            DataDescriptionVerifier.verify(dd);
            // shouldn't get here
            assertTrue("Invalid format should throw", false);
        }
        catch (JobConfigurationException e)
        {
        }

        String goodFormat = "yyyy.MM.dd G 'at' HH:mm:ss z";
        dd.setTimeFormat(goodFormat);

        assertTrue("Good time format", DataDescriptionVerifier.verify(dd));
    }
}
