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

package org.elasticsearch.xpack.watcher.actions.throttler;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.watcher.actions.ActionStatus;
import org.elasticsearch.xpack.watcher.execution.WatchExecutionContext;
import org.elasticsearch.xpack.support.clock.SystemClock;
import org.elasticsearch.xpack.watcher.watch.Watch;
import org.elasticsearch.xpack.watcher.watch.WatchStatus;
import org.joda.time.DateTime;

import static org.elasticsearch.xpack.watcher.support.WatcherDateTimeUtils.formatDate;
import static org.elasticsearch.xpack.watcher.test.WatcherTestUtils.EMPTY_PAYLOAD;
import static org.elasticsearch.xpack.watcher.test.WatcherTestUtils.mockExecutionContext;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class AckThrottlerTests extends ESTestCase {
    public void testWhenAcked() throws Exception {
        DateTime timestamp = SystemClock.INSTANCE.nowUTC();
        WatchExecutionContext ctx = mockExecutionContext("_watch", EMPTY_PAYLOAD);
        Watch watch = ctx.watch();
        ActionStatus actionStatus = mock(ActionStatus.class);
        when(actionStatus.ackStatus()).thenReturn(new ActionStatus.AckStatus(timestamp, ActionStatus.AckStatus.State.ACKED));
        WatchStatus watchStatus = mock(WatchStatus.class);
        when(watchStatus.actionStatus("_action")).thenReturn(actionStatus);
        when(watch.status()).thenReturn(watchStatus);
        AckThrottler throttler = new AckThrottler();
        Throttler.Result result = throttler.throttle("_action", ctx);
        assertThat(result.throttle(), is(true));
        assertThat(result.reason(), is("action [_action] was acked at [" + formatDate(timestamp) + "]"));
    }

    public void testThrottleWhenAwaitsSuccessfulExecution() throws Exception {
        DateTime timestamp = SystemClock.INSTANCE.nowUTC();
        WatchExecutionContext ctx = mockExecutionContext("_watch", EMPTY_PAYLOAD);
        Watch watch = ctx.watch();
        ActionStatus actionStatus = mock(ActionStatus.class);
        when(actionStatus.ackStatus()).thenReturn(new ActionStatus.AckStatus(timestamp,
                ActionStatus.AckStatus.State.AWAITS_SUCCESSFUL_EXECUTION));
        WatchStatus watchStatus = mock(WatchStatus.class);
        when(watchStatus.actionStatus("_action")).thenReturn(actionStatus);
        when(watch.status()).thenReturn(watchStatus);
        AckThrottler throttler = new AckThrottler();
        Throttler.Result result = throttler.throttle("_action", ctx);
        assertThat(result.throttle(), is(false));
        assertThat(result.reason(), nullValue());
    }

    public void testThrottleWhenAckable() throws Exception {
        DateTime timestamp = SystemClock.INSTANCE.nowUTC();
        WatchExecutionContext ctx = mockExecutionContext("_watch", EMPTY_PAYLOAD);
        Watch watch = ctx.watch();
        ActionStatus actionStatus = mock(ActionStatus.class);
        when(actionStatus.ackStatus()).thenReturn(new ActionStatus.AckStatus(timestamp, ActionStatus.AckStatus.State.ACKABLE));
        WatchStatus watchStatus = mock(WatchStatus.class);
        when(watchStatus.actionStatus("_action")).thenReturn(actionStatus);
        when(watch.status()).thenReturn(watchStatus);
        AckThrottler throttler = new AckThrottler();
        Throttler.Result result = throttler.throttle("_action", ctx);
        assertThat(result.throttle(), is(false));
        assertThat(result.reason(), nullValue());
    }
}
