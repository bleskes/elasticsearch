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

package org.elasticsearch.alerts.throttle;

import org.elasticsearch.alerts.ExecutionContext;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class AlertThrottlerTests extends ElasticsearchTestCase {

    @Test
    public void testThrottle_DueToAck() throws Exception {
        PeriodThrottler periodThrottler = mock(PeriodThrottler.class);
        AckThrottler ackThrottler = mock(AckThrottler.class);
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(periodThrottler.throttle(ctx)).thenReturn(Throttler.Result.NO);
        Throttler.Result expectedResult = Throttler.Result.throttle("_reason");
        when(ackThrottler.throttle(ctx)).thenReturn(expectedResult);
        AlertThrottler throttler = new AlertThrottler(periodThrottler, ackThrottler);
        Throttler.Result result = throttler.throttle(ctx);
        assertThat(result, notNullValue());
        assertThat(result, is(expectedResult));
    }

    @Test
    public void testThrottle_DueToPeriod() throws Exception {
        PeriodThrottler periodThrottler = mock(PeriodThrottler.class);
        AckThrottler ackThrottler = mock(AckThrottler.class);
        ExecutionContext ctx = mock(ExecutionContext.class);
        Throttler.Result expectedResult = Throttler.Result.throttle("_reason");
        when(periodThrottler.throttle(ctx)).thenReturn(expectedResult);
        when(ackThrottler.throttle(ctx)).thenReturn(Throttler.Result.NO);
        AlertThrottler throttler = new AlertThrottler(periodThrottler, ackThrottler);
        Throttler.Result result = throttler.throttle(ctx);
        assertThat(result, notNullValue());
        assertThat(result, is(expectedResult));
    }

    @Test
    public void testThrottle_DueBoth() throws Exception {
        PeriodThrottler periodThrottler = mock(PeriodThrottler.class);
        AckThrottler ackThrottler = mock(AckThrottler.class);
        ExecutionContext ctx = mock(ExecutionContext.class);
        Throttler.Result periodResult = Throttler.Result.throttle("_reason_period");
        when(periodThrottler.throttle(ctx)).thenReturn(periodResult);
        Throttler.Result ackResult = Throttler.Result.throttle("_reason_ack");
        when(ackThrottler.throttle(ctx)).thenReturn(ackResult);
        AlertThrottler throttler = new AlertThrottler(periodThrottler, ackThrottler);
        Throttler.Result result = throttler.throttle(ctx);
        assertThat(result, notNullValue());
        // we always check the period first... so the result will come for the period throttler
        assertThat(result, is(periodResult));
    }

    @Test
    public void testNoThrottle() throws Exception {
        PeriodThrottler periodThrottler = mock(PeriodThrottler.class);
        AckThrottler ackThrottler = mock(AckThrottler.class);
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(periodThrottler.throttle(ctx)).thenReturn(Throttler.Result.NO);
        when(ackThrottler.throttle(ctx)).thenReturn(Throttler.Result.NO);
        AlertThrottler throttler = new AlertThrottler(periodThrottler, ackThrottler);
        Throttler.Result result = throttler.throttle(ctx);
        assertThat(result, notNullValue());
        assertThat(result, is(Throttler.Result.NO));
    }

    @Test
    public void testWithoutPeriod() throws Exception {
        AckThrottler ackThrottler = mock(AckThrottler.class);
        ExecutionContext ctx = mock(ExecutionContext.class);
        Throttler.Result ackResult = mock(Throttler.Result.class);
        when(ackThrottler.throttle(ctx)).thenReturn(ackResult);
        AlertThrottler throttler = new AlertThrottler(null, ackThrottler);
        Throttler.Result result = throttler.throttle(ctx);
        assertThat(result, notNullValue());
        assertThat(result, sameInstance(ackResult));
    }
}
