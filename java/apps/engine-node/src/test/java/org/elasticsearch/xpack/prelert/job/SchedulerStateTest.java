
package org.elasticsearch.xpack.prelert.job;

import org.junit.Test;

import static org.junit.Assert.*;

public class SchedulerStateTest {
    @Test
    public void testEquals_GivenDifferentClass() {
        assertFalse(new SchedulerState().equals("a string"));
    }

    @Test
    public void testEquals_GivenSameReference() {
        SchedulerState schedulerState = new SchedulerState();
        assertTrue(schedulerState.equals(schedulerState));
    }

    @Test
    public void testEquals_GivenEqualObjects() {
        SchedulerState schedulerState1 = new SchedulerState();
        schedulerState1.setStartTimeMillis(18L);
        schedulerState1.setEndTimeMillis(42L);

        SchedulerState schedulerState2 = new SchedulerState(18L, 42L);

        assertTrue(schedulerState1.equals(schedulerState2));
        assertTrue(schedulerState2.equals(schedulerState1));
        assertEquals(schedulerState1.hashCode(), schedulerState2.hashCode());
    }
}
