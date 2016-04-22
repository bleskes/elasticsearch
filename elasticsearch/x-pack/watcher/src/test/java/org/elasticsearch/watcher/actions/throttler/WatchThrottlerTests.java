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

package org.elasticsearch.watcher.actions.throttler;

import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.watcher.execution.WatchExecutionContext;
import org.elasticsearch.watcher.WatcherLicensee;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class WatchThrottlerTests extends ESTestCase {
    public void testThrottleDueToAck() throws Exception {
        PeriodThrottler periodThrottler = mock(PeriodThrottler.class);
        AckThrottler ackThrottler = mock(AckThrottler.class);
        WatchExecutionContext ctx = mock(WatchExecutionContext.class);
        when(periodThrottler.throttle("_action", ctx)).thenReturn(Throttler.Result.NO);
        Throttler.Result expectedResult = Throttler.Result.throttle("_reason");
        when(ackThrottler.throttle("_action", ctx)).thenReturn(expectedResult);
        WatcherLicensee watcherLicensee = mock(WatcherLicensee.class);
        when(watcherLicensee.isExecutingActionsAllowed()).thenReturn(true);
        ActionThrottler throttler = new ActionThrottler(periodThrottler, ackThrottler, watcherLicensee);
        Throttler.Result result = throttler.throttle("_action", ctx);
        assertThat(result, notNullValue());
        assertThat(result, is(expectedResult));
    }

    public void testThrottleDueToPeriod() throws Exception {
        PeriodThrottler periodThrottler = mock(PeriodThrottler.class);
        AckThrottler ackThrottler = mock(AckThrottler.class);
        WatchExecutionContext ctx = mock(WatchExecutionContext.class);
        Throttler.Result expectedResult = Throttler.Result.throttle("_reason");
        when(periodThrottler.throttle("_action", ctx)).thenReturn(expectedResult);
        when(ackThrottler.throttle("_action", ctx)).thenReturn(Throttler.Result.NO);
        WatcherLicensee watcherLicensee = mock(WatcherLicensee.class);
        when(watcherLicensee.isExecutingActionsAllowed()).thenReturn(true);
        ActionThrottler throttler = new ActionThrottler(periodThrottler, ackThrottler, watcherLicensee);
        Throttler.Result result = throttler.throttle("_action", ctx);
        assertThat(result, notNullValue());
        assertThat(result, is(expectedResult));
    }

    public void testThrottleDueAckAndPeriod() throws Exception {
        PeriodThrottler periodThrottler = mock(PeriodThrottler.class);
        AckThrottler ackThrottler = mock(AckThrottler.class);
        WatchExecutionContext ctx = mock(WatchExecutionContext.class);
        Throttler.Result periodResult = Throttler.Result.throttle("_reason_period");
        when(periodThrottler.throttle("_action", ctx)).thenReturn(periodResult);
        Throttler.Result ackResult = Throttler.Result.throttle("_reason_ack");
        when(ackThrottler.throttle("_action", ctx)).thenReturn(ackResult);
        WatcherLicensee watcherLicensee = mock(WatcherLicensee.class);
        when(watcherLicensee.isExecutingActionsAllowed()).thenReturn(true);
        ActionThrottler throttler = new ActionThrottler(periodThrottler, ackThrottler, watcherLicensee);
        Throttler.Result result = throttler.throttle("_action", ctx);
        assertThat(result, notNullValue());
        // we always check the period first... so the result will come for the period throttler
        assertThat(result, is(periodResult));
    }

    public void testNoThrottle() throws Exception {
        PeriodThrottler periodThrottler = mock(PeriodThrottler.class);
        AckThrottler ackThrottler = mock(AckThrottler.class);
        WatchExecutionContext ctx = mock(WatchExecutionContext.class);
        when(periodThrottler.throttle("_action", ctx)).thenReturn(Throttler.Result.NO);
        when(ackThrottler.throttle("_action", ctx)).thenReturn(Throttler.Result.NO);
        WatcherLicensee watcherLicensee = mock(WatcherLicensee.class);
        when(watcherLicensee.isExecutingActionsAllowed()).thenReturn(true);
        ActionThrottler throttler = new ActionThrottler(periodThrottler, ackThrottler, watcherLicensee);
        Throttler.Result result = throttler.throttle("_action", ctx);
        assertThat(result, notNullValue());
        assertThat(result, is(Throttler.Result.NO));
    }

    public void testWithoutPeriod() throws Exception {
        AckThrottler ackThrottler = mock(AckThrottler.class);
        WatchExecutionContext ctx = mock(WatchExecutionContext.class);
        Throttler.Result ackResult = mock(Throttler.Result.class);
        when(ackThrottler.throttle("_action", ctx)).thenReturn(ackResult);
        WatcherLicensee watcherLicensee = mock(WatcherLicensee.class);
        when(watcherLicensee.isExecutingActionsAllowed()).thenReturn(true);
        ActionThrottler throttler = new ActionThrottler(null, ackThrottler, watcherLicensee);
        Throttler.Result result = throttler.throttle("_action", ctx);
        assertThat(result, notNullValue());
        assertThat(result, sameInstance(ackResult));
    }

    public void testThatRestrictedLicenseReturnsCorrectResult() throws Exception {
        AckThrottler ackThrottler = mock(AckThrottler.class);
        WatchExecutionContext ctx = mock(WatchExecutionContext.class);
        Throttler.Result ackResult = mock(Throttler.Result.class);
        when(ackThrottler.throttle("_action", ctx)).thenReturn(ackResult);
        WatcherLicensee watcherLicensee = mock(WatcherLicensee.class);
        when(watcherLicensee.isExecutingActionsAllowed()).thenReturn(false);
        ActionThrottler throttler = new ActionThrottler(null, ackThrottler, watcherLicensee);
        Throttler.Result result = throttler.throttle("_action", ctx);
        assertThat(result, notNullValue());
        assertThat(result.reason(), is("watcher license does not allow action execution"));
    }
}
