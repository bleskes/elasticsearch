
package org.elasticsearch.xpack.prelert.job.audit;

import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

public class AuditActivityTest {
    private long startMillis;

    @Before
    public void setUp() {
        startMillis = System.currentTimeMillis();
    }

    @Test
    public void testDefaultConstructor() {
        AuditActivity activity = new AuditActivity();
        assertEquals(0, activity.getTotalJobs());
        assertEquals(0, activity.getTotalDetectors());
        assertEquals(0, activity.getRunningJobs());
        assertEquals(0, activity.getRunningDetectors());
        assertNull(activity.getTimestamp());
    }

    @Test
    public void testNewActivity() {
        AuditActivity activity = AuditActivity.newActivity(10, 100, 5, 50);
        assertEquals(10, activity.getTotalJobs());
        assertEquals(100, activity.getTotalDetectors());
        assertEquals(5, activity.getRunningJobs());
        assertEquals(50, activity.getRunningDetectors());
        assertDateBetweenStartAndNow(activity.getTimestamp());
    }

    private void assertDateBetweenStartAndNow(Date timestamp) {
        long timestampMillis = timestamp.getTime();
        assertTrue(timestampMillis >= startMillis);
        assertTrue(timestampMillis <= System.currentTimeMillis());
    }
}
