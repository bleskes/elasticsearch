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

import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.support.clock.SystemClock;
import org.elasticsearch.xpack.watcher.actions.ActionStatus;
import org.elasticsearch.xpack.watcher.execution.WatchExecutionContext;
import org.elasticsearch.xpack.watcher.watch.Payload;
import org.elasticsearch.xpack.watcher.watch.WatchStatus;
import org.joda.time.PeriodType;

import static org.elasticsearch.xpack.watcher.test.WatcherTestUtils.mockExecutionContext;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class PeriodThrottlerTests extends ESTestCase {
    public void testBelowPeriodSuccessful() throws Exception {
        PeriodType periodType = randomFrom(PeriodType.millis(), PeriodType.seconds(), PeriodType.minutes());
        TimeValue period = TimeValue.timeValueSeconds(randomIntBetween(2, 5));
        PeriodThrottler throttler = new PeriodThrottler(SystemClock.INSTANCE, period, periodType);

        WatchExecutionContext ctx = mockExecutionContext("_name", Payload.EMPTY);
        ActionStatus actionStatus = mock(ActionStatus.class);
        when(actionStatus.lastSuccessfulExecution())
                .thenReturn(ActionStatus.Execution.successful(SystemClock.INSTANCE.nowUTC().minusSeconds((int) period.seconds() - 1)));
        WatchStatus status = mock(WatchStatus.class);
        when(status.actionStatus("_action")).thenReturn(actionStatus);
        when(ctx.watch().status()).thenReturn(status);

        Throttler.Result result = throttler.throttle("_action", ctx);
        assertThat(result, notNullValue());
        assertThat(result.throttle(), is(true));
        assertThat(result.reason(), notNullValue());
        assertThat(result.reason(), startsWith("throttling interval is set to [" + period.format(periodType) + "]"));
        assertThat(result.type(), is(Throttler.Type.PERIOD));
    }

    public void testAbovePeriod() throws Exception {
        PeriodType periodType = randomFrom(PeriodType.millis(), PeriodType.seconds(), PeriodType.minutes());
        TimeValue period = TimeValue.timeValueSeconds(randomIntBetween(2, 5));
        PeriodThrottler throttler = new PeriodThrottler(SystemClock.INSTANCE, period, periodType);

        WatchExecutionContext ctx = mockExecutionContext("_name", Payload.EMPTY);
        ActionStatus actionStatus = mock(ActionStatus.class);
        when(actionStatus.lastSuccessfulExecution())
                .thenReturn(ActionStatus.Execution.successful(SystemClock.INSTANCE.nowUTC().minusSeconds((int) period.seconds() + 1)));
        WatchStatus status = mock(WatchStatus.class);
        when(status.actionStatus("_action")).thenReturn(actionStatus);
        when(ctx.watch().status()).thenReturn(status);

        Throttler.Result result = throttler.throttle("_action", ctx);
        assertThat(result, notNullValue());
        assertThat(result.throttle(), is(false));
        assertThat(result.reason(), nullValue());
    }
}
