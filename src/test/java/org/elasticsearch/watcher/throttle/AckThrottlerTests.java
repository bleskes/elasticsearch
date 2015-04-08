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

package org.elasticsearch.watcher.throttle;

import org.elasticsearch.watcher.watch.Watch;
import org.elasticsearch.watcher.execution.WatchExecutionContext;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.junit.Test;

import static org.elasticsearch.watcher.support.WatcherDateUtils.formatDate;
import static org.elasticsearch.watcher.test.WatcherTestUtils.EMPTY_PAYLOAD;
import static org.elasticsearch.watcher.test.WatcherTestUtils.mockExecutionContext;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class AckThrottlerTests extends ElasticsearchTestCase {

    @Test
    public void testWhenAcked() throws Exception {
        DateTime timestamp = new DateTime();
        WatchExecutionContext ctx = mockExecutionContext("_watch", EMPTY_PAYLOAD);
        Watch watch = ctx.watch();
        Watch.Status status = mock(Watch.Status.class);
        when(status.ackStatus()).thenReturn(new Watch.Status.AckStatus(Watch.Status.AckStatus.State.ACKED, timestamp));
        when(watch.status()).thenReturn(status);
        when(watch.name()).thenReturn("_watch");
        when(watch.acked()).thenReturn(true);
        AckThrottler throttler = new AckThrottler();
        Throttler.Result result = throttler.throttle(ctx);
        assertThat(result.throttle(), is(true));
        assertThat(result.reason(), is("watch [_watch] was acked at [" + formatDate(timestamp) + "]"));
    }

    @Test
    public void testWhenNotAcked() throws Exception {
        DateTime timestamp = new DateTime();
        WatchExecutionContext ctx = mockExecutionContext("_watch", EMPTY_PAYLOAD);
        Watch watch = ctx.watch();
        Watch.Status status = mock(Watch.Status.class);
        Watch.Status.AckStatus.State state = randomFrom(Watch.Status.AckStatus.State.AWAITS_EXECUTION, Watch.Status.AckStatus.State.ACKABLE);
        when(status.ackStatus()).thenReturn(new Watch.Status.AckStatus(state, timestamp));
        when(watch.status()).thenReturn(status);
        when(watch.acked()).thenReturn(false);
        AckThrottler throttler = new AckThrottler();
        Throttler.Result result = throttler.throttle(ctx);
        assertThat(result.throttle(), is(false));
        assertThat(result.reason(), nullValue());
    }
}
