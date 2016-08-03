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
package com.prelert.job.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;

public class AuditMessageTest
{
    private long m_StartMillis;

    @Before
    public void setUp()
    {
        m_StartMillis = System.currentTimeMillis();
    }

    @Test
    public void testDefaultConstructor()
    {
        AuditMessage auditMessage = new AuditMessage();
        assertNull(auditMessage.getMessage());
        assertNull(auditMessage.getLevel());
        assertNull(auditMessage.getTimestamp());
    }

    @Test
    public void testNewInfo()
    {
        AuditMessage info = AuditMessage.newInfo("foo", "some info");
        assertEquals("foo", info.getJobId());
        assertEquals("some info", info.getMessage());
        assertEquals(Level.INFO, info.getLevel());
        assertDateBetweenStartAndNow(info.getTimestamp());
    }

    @Test
    public void testNewWarning()
    {
        AuditMessage warning = AuditMessage.newWarning("bar", "some warning");
        assertEquals("bar", warning.getJobId());
        assertEquals("some warning", warning.getMessage());
        assertEquals(Level.WARNING, warning.getLevel());
        assertDateBetweenStartAndNow(warning.getTimestamp());
    }

    @Test
    public void testNewError()
    {
        AuditMessage error = AuditMessage.newError("foo", "some error");
        assertEquals("foo", error.getJobId());
        assertEquals("some error", error.getMessage());
        assertEquals(Level.ERROR, error.getLevel());
        assertDateBetweenStartAndNow(error.getTimestamp());
    }

    @Test
    public void testNewActivity()
    {
        AuditMessage error = AuditMessage.newActivity("foo", "some error");
        assertEquals("foo", error.getJobId());
        assertEquals("some error", error.getMessage());
        assertEquals(Level.ACTIVITY, error.getLevel());
        assertDateBetweenStartAndNow(error.getTimestamp());
    }

    private void assertDateBetweenStartAndNow(Date timestamp)
    {
        long timestampMillis = timestamp.getTime();
        assertTrue(timestampMillis >= m_StartMillis);
        assertTrue(timestampMillis <= System.currentTimeMillis());
    }
}
