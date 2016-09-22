
package org.elasticsearch.xpack.prelert.job.manager.actions;

import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobInUseException;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class LocalActionGuardianTest {
    @Test
    public void testTryAcquiringAction_GivenAvailable() throws JobInUseException {
        LocalActionGuardian<Action> actionGuardian = new LocalActionGuardian<>(Action.startingState());
        try (ActionGuardian<Action>.ActionTicket actionTicket = actionGuardian.tryAcquiringAction("foo", Action.WRITING)) {
            assertEquals(Action.WRITING, actionGuardian.currentAction("foo"));
            assertEquals(Action.CLOSED, actionGuardian.currentAction("unknown"));
        }
        assertEquals(Action.SLEEPING, actionGuardian.currentAction("foo"));
    }

    @Test
    public void testTryAcquiringAction_GivenJobIsInUse() throws JobInUseException {
        LocalActionGuardian<Action> actionGuardian = new LocalActionGuardian<>(Action.CLOSED);
        try (ActionGuardian<Action>.ActionTicket deleting = actionGuardian.tryAcquiringAction("foo", Action.DELETING)) {
            try (ActionGuardian<Action>.ActionTicket writing = actionGuardian.tryAcquiringAction("foo", Action.WRITING)) {
                fail();
            } catch (JobInUseException e) {
                assertEquals("Cannot write to job foo while another connection is deleting the job", e.getMessage());
                assertEquals(ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR, e.getErrorCode());
            }
            assertEquals(Action.DELETING, actionGuardian.currentAction("foo"));
        }
        assertEquals(Action.CLOSED, actionGuardian.currentAction("foo"));
    }

    @Test
    public void testTryAcquiringAction_acquiresNextLock() throws JobInUseException {
        @SuppressWarnings("unchecked")
        ActionGuardian<Action> nextGuardian = Mockito.mock(ActionGuardian.class);

        ActionGuardian<Action> actionGuardian = new LocalActionGuardian<>(Action.CLOSED, nextGuardian);
        actionGuardian.tryAcquiringAction("foo", Action.CLOSING);

        Mockito.verify(nextGuardian).tryAcquiringAction("foo", Action.CLOSING);
    }

    @Test
    public void testTryAcquiringAction_releasesNextLock() throws JobInUseException {
        @SuppressWarnings("unchecked")
        ActionGuardian<Action> nextGuardian = Mockito.mock(ActionGuardian.class);

        ActionGuardian<Action> actionGuardian = new LocalActionGuardian<>(Action.CLOSED, nextGuardian);
        actionGuardian.releaseAction("foo", Action.CLOSED);

        Mockito.verify(nextGuardian).releaseAction("foo", Action.CLOSED);
    }

    @Test
    public void testReleasingLockTransitionsToNextState() throws JobInUseException {
        LocalActionGuardian<Action> actionGuardian = new LocalActionGuardian<>(Action.CLOSED);

        try (ActionGuardian<Action>.ActionTicket ticket =
                     actionGuardian.tryAcquiringAction("foo", Action.CLOSING)) {
        }
        assertEquals(Action.CLOSED, actionGuardian.currentAction("foo"));

        try (ActionGuardian<Action>.ActionTicket ticket =
                     actionGuardian.tryAcquiringAction("foo", Action.WRITING)) {
        }
        assertEquals(Action.SLEEPING, actionGuardian.currentAction("foo"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testActionIsnotSetIfNextGuardianFails() throws JobInUseException {
        ActionGuardian<ScheduledAction> next = Mockito.mock(ActionGuardian.class);
        LocalActionGuardian<ScheduledAction> actionGuardian =
                new LocalActionGuardian<>(ScheduledAction.startingState(), next);

        Mockito.when(next.tryAcquiringAction("foo", ScheduledAction.STARTED))
                .thenThrow(JobInUseException.class);

        try {
            actionGuardian.tryAcquiringAction("foo", ScheduledAction.STARTED);

            fail("Expected JobInUseException to be thrown");
        } catch (JobInUseException e) {
        }
        assertEquals(ScheduledAction.STOPPED, actionGuardian.currentAction("foo"));
    }
}
