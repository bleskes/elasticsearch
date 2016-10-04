
package org.elasticsearch.xpack.prelert.job.audit;

import org.elasticsearch.test.ESTestCase;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

public class AuditMessageTest extends ESTestCase {
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
