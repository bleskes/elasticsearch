
package org.elasticsearch.xpack.prelert.job.manager.actions;

import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.junit.Test;

import static org.junit.Assert.*;

public class ScheduledActionTest {
    @Test
    public void testTypename() {
        assertEquals("ScheduledAction", ScheduledAction.STOPPED.typename());
    }

    @Test
    public void testGetBusyActionError_GivenVariousActionsInUse() {
        assertEquals("Cannot start scheduler for job 'foo' while its status is started",
                ScheduledAction.STARTED.getBusyActionError("foo", ScheduledAction.STARTED));

        String g = ScheduledAction.STOPPED.getBusyActionError("foo", ScheduledAction.STARTED);
        assertEquals("Cannot stop scheduler for job 'foo' while its status is started", g);
    }

    @Test
    public void testGetBusyActionError_GivenHostAndVariousActionsInUse() {
        assertEquals("Cannot start scheduler for job 'foo' while its status is started on host marple",
                ScheduledAction.STARTED.getBusyActionError("foo", ScheduledAction.STARTED, "marple"));

        String msg = ScheduledAction.STOPPING.getBusyActionError("foo", ScheduledAction.STOPPING, "marple");
        assertEquals("Cannot stop scheduler for job 'foo' while its status is stopping on host marple", msg);

        msg = ScheduledAction.STOPPED.getBusyActionError("foo", ScheduledAction.STARTED, "marple");
        assertEquals("Cannot stop scheduler for job 'foo' while its status is started on host marple", msg);

        msg = ScheduledAction.UPDATE.getBusyActionError("foo", ScheduledAction.STARTED, "marple");
        assertEquals("Cannot update scheduler for job 'foo' while its status is started on host marple", msg);

        msg = ScheduledAction.DELETE.getBusyActionError("foo", ScheduledAction.STARTED, "marple");
        assertEquals("Cannot delete scheduler for job 'foo' while its status is started on host marple", msg);
    }

    @Test
    public void testIsValidTransition() {
        assertFalse(ScheduledAction.STARTED.isValidTransition(ScheduledAction.STARTED));
        assertTrue(ScheduledAction.STARTED.isValidTransition(ScheduledAction.STOPPING));
        assertFalse(ScheduledAction.STARTED.isValidTransition(ScheduledAction.STOPPED));
        assertFalse(ScheduledAction.STARTED.isValidTransition(ScheduledAction.UPDATE));
        assertTrue(ScheduledAction.STARTED.isValidTransition(ScheduledAction.DELETE));

        assertFalse(ScheduledAction.STOPPING.isValidTransition(ScheduledAction.STARTED));
        assertFalse(ScheduledAction.STOPPING.isValidTransition(ScheduledAction.STOPPING));
        assertTrue(ScheduledAction.STOPPING.isValidTransition(ScheduledAction.STOPPED));
        assertFalse(ScheduledAction.STOPPING.isValidTransition(ScheduledAction.UPDATE));
        assertFalse(ScheduledAction.STOPPING.isValidTransition(ScheduledAction.DELETE));

        assertTrue(ScheduledAction.STOPPED.isValidTransition(ScheduledAction.STARTED));
        assertTrue(ScheduledAction.STOPPED.isValidTransition(ScheduledAction.STOPPING));
        assertFalse(ScheduledAction.STOPPED.isValidTransition(ScheduledAction.STOPPED));
        assertTrue(ScheduledAction.STOPPED.isValidTransition(ScheduledAction.UPDATE));
        assertTrue(ScheduledAction.STOPPED.isValidTransition(ScheduledAction.DELETE));

        assertFalse(ScheduledAction.UPDATE.isValidTransition(ScheduledAction.STARTED));
        assertFalse(ScheduledAction.UPDATE.isValidTransition(ScheduledAction.STOPPING));
        assertTrue(ScheduledAction.UPDATE.isValidTransition(ScheduledAction.STOPPED));
        assertFalse(ScheduledAction.UPDATE.isValidTransition(ScheduledAction.UPDATE));
        assertFalse(ScheduledAction.UPDATE.isValidTransition(ScheduledAction.DELETE));

        assertFalse(ScheduledAction.DELETE.isValidTransition(ScheduledAction.STARTED));
        assertFalse(ScheduledAction.DELETE.isValidTransition(ScheduledAction.STOPPING));
        assertTrue(ScheduledAction.DELETE.isValidTransition(ScheduledAction.STOPPED));
        assertFalse(ScheduledAction.DELETE.isValidTransition(ScheduledAction.UPDATE));
        assertFalse(ScheduledAction.DELETE.isValidTransition(ScheduledAction.DELETE));
    }

    @Test
    public void testNextState() {
        assertEquals(ScheduledAction.STOPPED, ScheduledAction.STARTED.nextState(ScheduledAction.STOPPED));
        assertEquals(ScheduledAction.STOPPED, ScheduledAction.STARTED.nextState(ScheduledAction.STARTED));
        assertEquals(ScheduledAction.STOPPED, ScheduledAction.STOPPING.nextState(ScheduledAction.STARTED));
        assertEquals(ScheduledAction.STOPPED, ScheduledAction.STOPPING.nextState(ScheduledAction.STOPPED));
        assertEquals(ScheduledAction.STOPPED, ScheduledAction.STOPPED.nextState(ScheduledAction.STARTED));
        assertEquals(ScheduledAction.STOPPED, ScheduledAction.STOPPED.nextState(ScheduledAction.STOPPED));
        assertEquals(ScheduledAction.STOPPED, ScheduledAction.UPDATE.nextState(ScheduledAction.STOPPED));
        assertEquals(ScheduledAction.STOPPED, ScheduledAction.UPDATE.nextState(ScheduledAction.DELETE));
        assertEquals(ScheduledAction.STOPPED, ScheduledAction.DELETE.nextState(ScheduledAction.STOPPED));
        assertEquals(ScheduledAction.STOPPED, ScheduledAction.DELETE.nextState(ScheduledAction.STARTED));
    }

    @Test
    public void testHoldDistributedLock() {
        assertTrue(ScheduledAction.STARTED.holdDistributedLock());
        assertTrue(ScheduledAction.STOPPING.holdDistributedLock());
        assertFalse(ScheduledAction.STOPPED.holdDistributedLock());
        assertFalse(ScheduledAction.UPDATE.holdDistributedLock());
        assertFalse(ScheduledAction.DELETE.holdDistributedLock());
    }

    @Test
    public void testStartingState() {
        assertEquals(ScheduledAction.STOPPED, ScheduledAction.startingState());
    }

    @Test
    public void testErrorCode() {
        assertEquals(ErrorCodes.CANNOT_START_JOB_SCHEDULER, ScheduledAction.STARTED.getErrorCode());
        assertEquals(ErrorCodes.CANNOT_STOP_JOB_SCHEDULER, ScheduledAction.STOPPING.getErrorCode());
        assertEquals(ErrorCodes.CANNOT_STOP_JOB_SCHEDULER, ScheduledAction.STOPPED.getErrorCode());
        assertEquals(ErrorCodes.CANNOT_UPDATE_JOB_SCHEDULER, ScheduledAction.UPDATE.getErrorCode());
        assertEquals(ErrorCodes.CANNOT_DELETE_JOB_SCHEDULER, ScheduledAction.DELETE.getErrorCode());
    }
}
