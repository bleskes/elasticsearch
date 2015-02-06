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
import org.elasticsearch.alerts.AlertContext;
import org.elasticsearch.alerts.trigger.Trigger;
import org.elasticsearch.common.joda.time.PeriodType;
import org.elasticsearch.common.unit.TimeValue;

/**
 *
 */
public class PeriodThrottler implements Throttler {

    private final TimeValue period;
    private final PeriodType periodType;

    public PeriodThrottler(TimeValue period) {
        this(period, PeriodType.minutes());
    }

    public PeriodThrottler(TimeValue period, PeriodType periodType) {
        this.period = period;
        this.periodType = periodType;
    }

    public TimeValue interval() {
        return period;
    }

    @Override
    public Result throttle(AlertContext ctx, Trigger.Result result) {
        Alert.Status status = ctx.alert().status();
        if (status.lastRan() != null) {
            TimeValue timeElapsed = new TimeValue(System.currentTimeMillis() - status.lastExecuted().getMillis());
            if (timeElapsed.getMillis() <= period.getMillis()) {
                return Result.throttle("throttling interval is set to [" + period.format(periodType) +
                        "] but time elapsed since last execution is [" + timeElapsed.format(periodType) + "]");
            }
        }
        return Result.NO;
    }
}
