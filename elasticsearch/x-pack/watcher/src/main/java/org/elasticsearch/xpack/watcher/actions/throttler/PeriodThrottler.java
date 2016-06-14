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

import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.xpack.watcher.actions.ActionStatus;
import org.elasticsearch.xpack.watcher.execution.WatchExecutionContext;
import org.elasticsearch.xpack.support.clock.Clock;
import org.joda.time.PeriodType;

/**
 * This throttler throttles the action based on its last <b>successful</b> execution time. If the time passed since
 * the last successful execution is lower than the given period, the aciton will be throttled.
 */
public class PeriodThrottler implements Throttler {

    private final @Nullable TimeValue period;
    private final PeriodType periodType;
    private final Clock clock;

    public PeriodThrottler(Clock clock, TimeValue period) {
        this(clock, period, PeriodType.minutes());
    }

    public PeriodThrottler(Clock clock, TimeValue period, PeriodType periodType) {
        this.period = period;
        this.periodType = periodType;
        this.clock = clock;
    }

    public TimeValue period() {
        return period;
    }

    @Override
    public Result throttle(String actionId, WatchExecutionContext ctx) {
        TimeValue period = this.period;
        if (period == null) {
            // falling back on the throttle period of the watch
            period = ctx.watch().throttlePeriod();
        }
        if (period == null) {
            // falling back on the default throttle period of watcher
            period = ctx.defaultThrottlePeriod();
        }
        ActionStatus status = ctx.watch().status().actionStatus(actionId);
        if (status.lastSuccessfulExecution() == null) {
            return Result.NO;
        }
        TimeValue timeElapsed = clock.timeElapsedSince(status.lastSuccessfulExecution().timestamp());
        if (timeElapsed.getMillis() <= period.getMillis()) {
            return Result.throttle("throttling interval is set to [{}] but time elapsed since last execution is [{}]",
                    period.format(periodType), timeElapsed.format(periodType));
        }
        return Result.NO;
    }
}
