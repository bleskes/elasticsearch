/****************************************************************************
 *                                                                          *
 * Copyright 2015-2016 Prelert Ltd                                          *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *    http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 *                                                                          *
 ***************************************************************************/

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
