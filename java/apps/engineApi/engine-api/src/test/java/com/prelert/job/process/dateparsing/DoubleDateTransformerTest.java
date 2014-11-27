/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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

package com.prelert.job.process.dateparsing;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DoubleDateTransformerTest
{

    @Rule
    public ExpectedException m_ExpectedException = ExpectedException.none();

    @Test
    public void testTransform_GivenTimestampIsNotMilliseconds() throws CannotParseTimestampException
    {
        DoubleDateTransformer transformer = new DoubleDateTransformer(false);

        assertEquals(1000, transformer.transform("1000"));
    }

    @Test
    public void testTransform_GivenTimestampIsMilliseconds() throws CannotParseTimestampException
    {
        DoubleDateTransformer transformer = new DoubleDateTransformer(true);

        assertEquals(1, transformer.transform("1000"));
    }

    @Test
    public void testTransform_GivenTimestampIsNotValidDouble() throws CannotParseTimestampException
    {
        m_ExpectedException.expect(CannotParseTimestampException.class);
        m_ExpectedException.expectMessage("Cannot parse timestamp 'invalid' as epoch value");

        DoubleDateTransformer transformer = new DoubleDateTransformer(false);

        assertEquals(1000, transformer.transform("invalid"));
    }
}
