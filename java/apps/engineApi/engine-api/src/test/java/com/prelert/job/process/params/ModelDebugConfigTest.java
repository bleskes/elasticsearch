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

package com.prelert.job.process.params;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobConfigurationException;

public class ModelDebugConfigTest
{
    @Rule public ExpectedException m_ExpectedException = ExpectedException.none();

    @Test
    public void testVerify_GivenBoundPercentileLessThanZero() throws JobConfigurationException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage(
                "Invalid modelDebugConfig: boundPercentile has to be in [0, 100]");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));
        new ModelDebugConfig(-1.0, "").verify();
    }

    @Test
    public void testVerify_GivenBoundPercentileGreaterThan100() throws JobConfigurationException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage(
                "Invalid modelDebugConfig: boundPercentile has to be in [0, 100]");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));
        new ModelDebugConfig(100.1, "").verify();
    }

    @Test
    public void testVerify_GivenValid() throws JobConfigurationException
    {
        assertTrue(new ModelDebugConfig().verify());
        assertTrue(new ModelDebugConfig(93.0, "").verify());
        assertTrue(new ModelDebugConfig(93.0, "foo,bar").verify());
    }

    @Test
    public void testEquals()
    {
        assertFalse(new ModelDebugConfig().equals(null));
        assertFalse(new ModelDebugConfig().equals("a string"));
        assertFalse(new ModelDebugConfig(80.0, "").equals(new ModelDebugConfig(81.0, "")));
        assertFalse(new ModelDebugConfig(80.0, "foo").equals(new ModelDebugConfig(80.0, "bar")));

        ModelDebugConfig modelDebugConfig = new ModelDebugConfig();
        assertTrue(modelDebugConfig.equals(modelDebugConfig));
        assertTrue(new ModelDebugConfig().equals(new ModelDebugConfig()));
        assertTrue(new ModelDebugConfig(80.0, "foo").equals(new ModelDebugConfig(80.0, "foo")));
    }

    @Test
    public void testHashCode()
    {
        assertEquals(new ModelDebugConfig(80.0, "foo").hashCode(),
                new ModelDebugConfig(80.0, "foo").hashCode());
    }
}
