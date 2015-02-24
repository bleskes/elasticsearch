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

import org.elasticsearch.alerts.Alert;
import org.elasticsearch.alerts.ExecutionContext;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.junit.Test;

import static org.elasticsearch.alerts.support.AlertsDateUtils.formatDate;
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
        ExecutionContext ctx = mock(ExecutionContext.class);
        Alert alert = mock(Alert.class);
        Alert.Status status = mock(Alert.Status.class);
        when(status.ackStatus()).thenReturn(new Alert.Status.AckStatus(Alert.Status.AckStatus.State.ACKED, timestamp));
        when(alert.status()).thenReturn(status);
        when(alert.name()).thenReturn("_alert");
        when(alert.acked()).thenReturn(true);
        when(ctx.alert()).thenReturn(alert);
        AckThrottler throttler = new AckThrottler();
        Throttler.Result result = throttler.throttle(ctx);
        assertThat(result.throttle(), is(true));
        assertThat(result.reason(), is("alert [_alert] was acked at [" + formatDate(timestamp) + "]"));
    }

    @Test
    public void testWhenNotAcked() throws Exception {
        DateTime timestamp = new DateTime();
        ExecutionContext ctx = mock(ExecutionContext.class);
        Alert alert = mock(Alert.class);
        Alert.Status status = mock(Alert.Status.class);
        Alert.Status.AckStatus.State state = randomFrom(Alert.Status.AckStatus.State.AWAITS_EXECUTION, Alert.Status.AckStatus.State.ACKABLE);
        when(status.ackStatus()).thenReturn(new Alert.Status.AckStatus(state, timestamp));
        when(alert.status()).thenReturn(status);
        when(alert.name()).thenReturn("_alert");
        when(alert.acked()).thenReturn(false);
        when(ctx.alert()).thenReturn(alert);
        AckThrottler throttler = new AckThrottler();
        Throttler.Result result = throttler.throttle(ctx);
        assertThat(result.throttle(), is(false));
        assertThat(result.reason(), nullValue());
    }
}
