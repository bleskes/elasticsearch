/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.prelert.job.audit;

import org.elasticsearch.test.ESTestCase;
import org.junit.Before;

import java.util.Date;

public class AuditMessageTests extends ESTestCase {
    private long startMillis;

    @Before
    public void setStartTime() {
        startMillis = System.currentTimeMillis();
    }

    public void testDefaultConstructor() {
        AuditMessage auditMessage = new AuditMessage();
        assertNull(auditMessage.getMessage());
        assertNull(auditMessage.getLevel());
        assertNull(auditMessage.getTimestamp());
    }

    public void testNewInfo() {
        AuditMessage info = AuditMessage.newInfo("foo", "some info");
        assertEquals("foo", info.getJobId());
        assertEquals("some info", info.getMessage());
        assertEquals(Level.INFO, info.getLevel());
        assertDateBetweenStartAndNow(info.getTimestamp());
    }

    public void testNewWarning() {
        AuditMessage warning = AuditMessage.newWarning("bar", "some warning");
        assertEquals("bar", warning.getJobId());
        assertEquals("some warning", warning.getMessage());
        assertEquals(Level.WARNING, warning.getLevel());
        assertDateBetweenStartAndNow(warning.getTimestamp());
    }


    public void testNewError() {
        AuditMessage error = AuditMessage.newError("foo", "some error");
        assertEquals("foo", error.getJobId());
        assertEquals("some error", error.getMessage());
        assertEquals(Level.ERROR, error.getLevel());
        assertDateBetweenStartAndNow(error.getTimestamp());
    }

    public void testNewActivity() {
        AuditMessage error = AuditMessage.newActivity("foo", "some error");
        assertEquals("foo", error.getJobId());
        assertEquals("some error", error.getMessage());
        assertEquals(Level.ACTIVITY, error.getLevel());
        assertDateBetweenStartAndNow(error.getTimestamp());
    }

    private void assertDateBetweenStartAndNow(Date timestamp) {
        long timestampMillis = timestamp.getTime();
        assertTrue(timestampMillis >= startMillis);
        assertTrue(timestampMillis <= System.currentTimeMillis());
    }
}
