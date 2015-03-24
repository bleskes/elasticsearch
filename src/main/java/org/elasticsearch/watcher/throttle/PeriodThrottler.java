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
import org.elasticsearch.watcher.watch.WatchExecutionContext;
import org.elasticsearch.watcher.support.clock.Clock;
import org.elasticsearch.common.joda.time.PeriodType;
import org.elasticsearch.common.unit.TimeValue;

/**
 *
 */
public class PeriodThrottler implements Throttler {

    private final TimeValue period;
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

    public TimeValue interval() {
        return period;
    }

    @Override
    public Result throttle(WatchExecutionContext ctx) {
        Watch.Status status = ctx.watch().status();
        if (status.lastExecuted() != null) {
            TimeValue timeElapsed = clock.timeElapsedSince(status.lastExecuted());
            if (timeElapsed.getMillis() <= period.getMillis()) {
                return Result.throttle("throttling interval is set to [" + period.format(periodType) +
                        "] but time elapsed since last execution is [" + timeElapsed.format(periodType) + "]");
            }
        }
        return Result.NO;
    }
}
