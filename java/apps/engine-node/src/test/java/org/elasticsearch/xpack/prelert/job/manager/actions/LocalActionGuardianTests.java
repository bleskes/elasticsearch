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
package org.elasticsearch.xpack.prelert.job.manager.actions;

import org.elasticsearch.test.ESTestCase;
import org.mockito.Mockito;

import java.util.concurrent.RejectedExecutionException;

public class LocalActionGuardianTests extends ESTestCase {

    public void testTryAcquiringAction_GivenAvailable() {
        LocalActionGuardian<Action> actionGuardian = new LocalActionGuardian<>(Action.startingState());
        try (ActionGuardian<Action>.ActionTicket actionTicket = actionGuardian.tryAcquiringAction("foo", Action.WRITING)) {
            assertEquals(Action.WRITING, actionGuardian.currentAction("foo"));
            assertEquals(Action.CLOSED, actionGuardian.currentAction("unknown"));
            actionTicket.hashCode();
        }
        assertEquals(Action.SLEEPING, actionGuardian.currentAction("foo"));
    }


    public void testTryAcquiringAction_GivenJobIsInUse() {
        LocalActionGuardian<Action> actionGuardian = new LocalActionGuardian<>(Action.CLOSED);
        try (ActionGuardian<Action>.ActionTicket deleting = actionGuardian.tryAcquiringAction("foo", Action.DELETING)) {
            try (ActionGuardian<Action>.ActionTicket writing = actionGuardian.tryAcquiringAction("foo", Action.WRITING)) {
                fail();
                writing.hashCode();
            } catch (RejectedExecutionException e) {
                assertEquals("Cannot write to job foo while another connection is deleting the job", e.getMessage());
            }
            assertEquals(Action.DELETING, actionGuardian.currentAction("foo"));
            deleting.hashCode();

        }
        assertEquals(Action.CLOSED, actionGuardian.currentAction("foo"));
    }


    public void testTryAcquiringAction_acquiresNextLock() {
        @SuppressWarnings("unchecked")
        ActionGuardian<Action> nextGuardian = Mockito.mock(ActionGuardian.class);

        ActionGuardian<Action> actionGuardian = new LocalActionGuardian<>(Action.CLOSED, nextGuardian);
        actionGuardian.tryAcquiringAction("foo", Action.CLOSING);

        Mockito.verify(nextGuardian).tryAcquiringAction("foo", Action.CLOSING);
    }


    public void testTryAcquiringAction_releasesNextLock() {
        @SuppressWarnings("unchecked")
        ActionGuardian<Action> nextGuardian = Mockito.mock(ActionGuardian.class);

        ActionGuardian<Action> actionGuardian = new LocalActionGuardian<>(Action.CLOSED, nextGuardian);
        actionGuardian.releaseAction("foo", Action.CLOSED);

        Mockito.verify(nextGuardian).releaseAction("foo", Action.CLOSED);
    }


    public void testReleasingLockTransitionsToNextState() {
        LocalActionGuardian<Action> actionGuardian = new LocalActionGuardian<>(Action.CLOSED);

        try (ActionGuardian<Action>.ActionTicket ticket =
                actionGuardian.tryAcquiringAction("foo", Action.CLOSING)) {
            ticket.hashCode();
        }
        assertEquals(Action.CLOSED, actionGuardian.currentAction("foo"));

        try (ActionGuardian<Action>.ActionTicket ticket =
                actionGuardian.tryAcquiringAction("foo", Action.WRITING)) {
            ticket.hashCode();
        }
        assertEquals(Action.SLEEPING, actionGuardian.currentAction("foo"));
    }
}
