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

public class DateFormatDateTransformerTest
{

    @Rule
    public ExpectedException m_ExpectedException = ExpectedException.none();

    @Test
    public void testTransform_GivenValidTimestamp() throws CannotParseTimestampException
    {
        DateFormatDateTransformer transformer = new DateFormatDateTransformer("y-M-d");

        assertEquals(1388534400, transformer.transform("2014-01-01"));
    }

    @Test
    public void testTransform_GivenInvalidTimestamp() throws CannotParseTimestampException
    {
        m_ExpectedException.expect(CannotParseTimestampException.class);
        m_ExpectedException.expectMessage("Cannot parse date 'invalid' with format string 'y-M-d'");

        DateFormatDateTransformer transformer = new DateFormatDateTransformer("y-M-d");

        assertEquals(1388534400, transformer.transform("invalid"));
    }
}
