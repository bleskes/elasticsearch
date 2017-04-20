/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */

package org.elasticsearch.xpack.watcher.watch;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.watcher.actions.ActionStatus;
import org.elasticsearch.xpack.watcher.actions.ActionStatus.AckStatus.State;
import org.elasticsearch.xpack.watcher.actions.logging.LoggingAction;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.joda.time.DateTime.now;

public class WatchStatusTests extends ESTestCase {

    public void testThatWatchStatusDirtyOnConditionCheck() throws Exception {
        // no actions, met condition
        WatchStatus status = new WatchStatus(now(), new HashMap<>());
        status.onCheck(true, now());
        assertThat(status.dirty(), is(true));

        // no actions, unmet condition
        status = new WatchStatus(now(), new HashMap<>());
        status.onCheck(false, now());
        assertThat(status.dirty(), is(true));

        // actions, no action with reset ack status, unmet condition
        Map<String, ActionStatus > actions = new HashMap<>();
        actions.put(randomAlphaOfLength(10), new ActionStatus(now()));
        status = new WatchStatus(now(), actions);
        status.onCheck(false, now());
        assertThat(status.dirty(), is(true));

        // actions, one action with state other than AWAITS_SUCCESSFUL_EXECUTION, unmet condition
        actions.clear();
        ActionStatus.AckStatus ackStatus = new ActionStatus.AckStatus(now(), randomFrom(State.ACKED, State.ACKABLE));
        actions.put(randomAlphaOfLength(10), new ActionStatus(ackStatus, null, null, null));
        actions.put(randomAlphaOfLength(11), new ActionStatus(now()));
        status = new WatchStatus(now(), actions);
        status.onCheck(false, now());
        assertThat(status.dirty(), is(true));

        status.resetDirty();
        assertThat(status.dirty(), is(false));
    }

    public void testDeepCopyingStatusWorks() throws Exception {
        HashMap<String, ActionStatus> myMap = new HashMap<>();
        ActionStatus actionStatus = new ActionStatus(now());
        myMap.put("foo", actionStatus);

        actionStatus.update(now(), new LoggingAction.Result.Success("foo"));
        assertThat(actionStatus.ackStatus().state(), is(State.ACKABLE));

        WatchStatus w1 = new WatchStatus(now(), myMap);
        WatchStatus w2 = new WatchStatus(w1);
        assertThat(w2.actionStatus("foo").ackStatus(), sameInstance(w1.actionStatus("foo").ackStatus()));

        WatchStatus copyW1 = WatchStatus.deepCopy(w1);
        WatchStatus copyW2 = WatchStatus.deepCopy(w2);
        assertThat(copyW1.actionStatus("foo").ackStatus(), not(sameInstance(w1.actionStatus("foo").ackStatus())));
        assertThat(copyW2.actionStatus("foo").ackStatus(), not(sameInstance(w2.actionStatus("foo").ackStatus())));
        assertThat(copyW2.actionStatus("foo").ackStatus(), not(sameInstance(copyW1.actionStatus("foo").ackStatus())));

        // equality check should be true
        assertThat(w1, equalTo(w2));
        assertThat(copyW1, equalTo(w1));
        assertThat(copyW2, equalTo(w2));

        // hashcode should check, should also be the same
        assertThat(w1.hashCode(), is(w2.hashCode()));
        assertThat(copyW1.hashCode(), is(w1.hashCode()));
        assertThat(copyW2.hashCode(), is(w2.hashCode()));
    }

    public void testAckStatusIsResetOnUnmetCondition() {
        HashMap<String, ActionStatus> myMap = new HashMap<>();
        ActionStatus actionStatus = new ActionStatus(now());
        myMap.put("foo", actionStatus);

        actionStatus.update(now(), new LoggingAction.Result.Success("foo"));
        actionStatus.onAck(now());
        assertThat(actionStatus.ackStatus().state(), is(State.ACKED));

        WatchStatus status = new WatchStatus(now(), myMap);
        status.onCheck(false, now());

        assertThat(status.actionStatus("foo").ackStatus().state(), is(State.AWAITS_SUCCESSFUL_EXECUTION));
    }
}