
package org.elasticsearch.xpack.prelert.job.manager.actions;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;

public class ActionTests extends ESTestCase {

    public void testTypename() {
        assertEquals("Action", Action.CLOSED.typename());
    }


    public void testgetBusyActionError_GivenVariousActionsInUse() {
        assertEquals("Cannot close job foo while another connection is closing the job",
                Action.CLOSING.getBusyActionError("foo", Action.CLOSING));
        assertEquals("Cannot close job foo while another connection is deleting the job",
                Action.CLOSING.getBusyActionError("foo", Action.DELETING));
        assertEquals("Cannot close job bar while another connection is flushing the job",
                Action.CLOSING.getBusyActionError("bar", Action.FLUSHING));
        assertEquals("Cannot close job bar while another connection is pausing the job",
                Action.CLOSING.getBusyActionError("bar", Action.PAUSING));
        assertEquals("Cannot close job bar while another connection is resuming the job",
                Action.CLOSING.getBusyActionError("bar", Action.RESUMING));
        assertEquals("Cannot close job bar while another connection is reverting the model snapshot for the job",
                Action.CLOSING.getBusyActionError("bar", Action.REVERTING));
        assertEquals("Cannot close job foo while another connection is updating the job",
                Action.CLOSING.getBusyActionError("foo", Action.UPDATING));
        assertEquals("Cannot close job foo while another connection is writing to the job",
                Action.CLOSING.getBusyActionError("foo", Action.WRITING));
    }


    public void testgetBusyActionError_GivenVariousActions() {
        assertEquals("Cannot close job foo while another connection is flushing the job",
                Action.CLOSING.getBusyActionError("foo", Action.FLUSHING));
        assertEquals("Cannot delete job foo while another connection is flushing the job",
                Action.DELETING.getBusyActionError("foo", Action.FLUSHING));
        assertEquals("Cannot flush job bar while another connection is flushing the job",
                Action.FLUSHING.getBusyActionError("bar", Action.FLUSHING));
        assertEquals("Cannot pause job foo while another connection is flushing the job",
                Action.PAUSING.getBusyActionError("foo", Action.FLUSHING));
        assertEquals("Cannot resume job foo while another connection is flushing the job",
                Action.RESUMING.getBusyActionError("foo", Action.FLUSHING));
        assertEquals("Cannot revert model snapshot for job foo while another connection is flushing the job",
                Action.REVERTING.getBusyActionError("foo", Action.FLUSHING));
        assertEquals("Cannot update job foo while another connection is flushing the job",
                Action.UPDATING.getBusyActionError("foo", Action.FLUSHING));
        assertEquals("Cannot write to job foo while another connection is flushing the job",
                Action.WRITING.getBusyActionError("foo", Action.FLUSHING));
    }


    public void testgetBusyActionErrorWithHost_GivenVariousActions() {
        assertEquals("Cannot close job foo while another connection on host marple is flushing the job",
                Action.CLOSING.getBusyActionError("foo", Action.FLUSHING, "marple"));
        assertEquals("Cannot delete job foo while another connection on host marple is flushing the job",
                Action.DELETING.getBusyActionError("foo", Action.FLUSHING, "marple"));
        assertEquals("Cannot flush job bar while another connection on host marple is flushing the job",
                Action.FLUSHING.getBusyActionError("bar", Action.FLUSHING, "marple"));
        assertEquals("Cannot pause job foo while another connection on host marple is flushing the job",
                Action.PAUSING.getBusyActionError("foo", Action.FLUSHING, "marple"));
        assertEquals("Cannot resume job foo while another connection on host marple is flushing the job",
                Action.RESUMING.getBusyActionError("foo", Action.FLUSHING, "marple"));
        assertEquals("Cannot revert model snapshot for job foo while another connection on host marple is flushing the job",
                Action.REVERTING.getBusyActionError("foo", Action.FLUSHING, "marple"));
        assertEquals("Cannot update job foo while another connection on host marple is flushing the job",
                Action.UPDATING.getBusyActionError("foo", Action.FLUSHING, "marple"));
        assertEquals("Cannot write to job foo while another connection on host marple is flushing the job",
                Action.WRITING.getBusyActionError("foo", Action.FLUSHING, "marple"));
    }


    public void testIsValidTransition_WhenClosed() {
        Action currentAction = Action.CLOSED;
        for (Action nextAction : Action.values()) {
            assertTrue(currentAction.isValidTransition(nextAction));
        }
    }


    public void testIsValidTransition_FalseWhenNotClosedOrSleeping() {
        for (Action currentAction : Action.values()) {
            if (currentAction == Action.CLOSED || currentAction == Action.SLEEPING) {
                continue;
            }

            for (Action nextAction : Action.values()) {
                assertFalse(currentAction.isValidTransition(nextAction));
            }
        }
    }


    public void testIsValidTransition_ValidStatesFromSleeping() {
        assertTrue(Action.SLEEPING.isValidTransition(Action.UPDATING));
        assertTrue(Action.SLEEPING.isValidTransition(Action.FLUSHING));
        assertTrue(Action.SLEEPING.isValidTransition(Action.CLOSING));
        assertTrue(Action.SLEEPING.isValidTransition(Action.DELETING));
        assertTrue(Action.SLEEPING.isValidTransition(Action.WRITING));
        assertTrue(Action.SLEEPING.isValidTransition(Action.PAUSING));

        assertFalse(Action.SLEEPING.isValidTransition(Action.RESUMING));
        assertFalse(Action.SLEEPING.isValidTransition(Action.REVERTING));
        assertFalse(Action.SLEEPING.isValidTransition(Action.CLOSED));
    }


    public void testNextState() {
        assertEquals(Action.CLOSED, Action.CLOSED.nextState(Action.CLOSED));
        assertEquals(Action.CLOSED, Action.CLOSING.nextState(Action.SLEEPING));
        assertEquals(Action.CLOSED, Action.DELETING.nextState(Action.SLEEPING));
        assertEquals(Action.CLOSED, Action.PAUSING.nextState(Action.SLEEPING));
        assertEquals(Action.CLOSED, Action.RESUMING.nextState(Action.CLOSED));
        assertEquals(Action.CLOSED, Action.REVERTING.nextState(Action.CLOSED));
        assertEquals(Action.SLEEPING, Action.SLEEPING.nextState(Action.WRITING));
        assertEquals(Action.SLEEPING, Action.FLUSHING.nextState(Action.SLEEPING));
        assertEquals(Action.SLEEPING, Action.UPDATING.nextState(Action.SLEEPING));
        assertEquals(Action.CLOSED, Action.UPDATING.nextState(Action.CLOSED));
        assertEquals(Action.SLEEPING, Action.WRITING.nextState(Action.SLEEPING));
    }


    public void testHoldDistributedLock() {
        assertFalse(Action.CLOSED.holdDistributedLock());
        assertFalse(Action.CLOSING.holdDistributedLock());
        assertFalse(Action.DELETING.holdDistributedLock());
        assertFalse(Action.PAUSING.holdDistributedLock());
        assertFalse(Action.RESUMING.holdDistributedLock());
        assertFalse(Action.REVERTING.holdDistributedLock());
        assertTrue(Action.SLEEPING.holdDistributedLock());
        assertFalse(Action.FLUSHING.holdDistributedLock());
        assertFalse(Action.UPDATING.holdDistributedLock());
        assertFalse(Action.UPDATING.holdDistributedLock());
        assertFalse(Action.WRITING.holdDistributedLock());
    }


    public void testStartingState() {
        assertEquals(Action.CLOSED, Action.startingState());
    }


    public void testErrorCode() {
        assertEquals(ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR, Action.CLOSED.getErrorCode());
        assertEquals(ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR, Action.CLOSING.getErrorCode());
        assertEquals(ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR, Action.DELETING.getErrorCode());
        assertEquals(ErrorCodes.CANNOT_PAUSE_JOB, Action.PAUSING.getErrorCode());
        assertEquals(ErrorCodes.CANNOT_RESUME_JOB, Action.RESUMING.getErrorCode());
        assertEquals(ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR, Action.REVERTING.getErrorCode());
        assertEquals(ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR, Action.SLEEPING.getErrorCode());
        assertEquals(ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR, Action.FLUSHING.getErrorCode());
        assertEquals(ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR, Action.UPDATING.getErrorCode());
        assertEquals(ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR, Action.UPDATING.getErrorCode());
        assertEquals(ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR, Action.WRITING.getErrorCode());
    }
}
