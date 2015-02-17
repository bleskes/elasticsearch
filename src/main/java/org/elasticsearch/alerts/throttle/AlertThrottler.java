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
import org.elasticsearch.alerts.condition.Condition;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.unit.TimeValue;

/**
 *
 */
public class AlertThrottler implements Throttler {

    private static final AckThrottler ACK_THROTTLER = new AckThrottler();

    private final PeriodThrottler periodThrottler;

    public AlertThrottler(@Nullable TimeValue throttlePeriod) {
        this.periodThrottler = throttlePeriod != null ? new PeriodThrottler(throttlePeriod) : null;
    }

    @Override
    public Result throttle(ExecutionContext ctx, Condition.Result result) {
        if (periodThrottler != null) {
            Result throttleResult = periodThrottler.throttle(ctx, result);
            if (throttleResult.throttle()) {
                return throttleResult;
            }
        }
        return ACK_THROTTLER.throttle(ctx, result);
    }
}
