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
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.xpack.watcher.execution.WatchExecutionContext;
import org.elasticsearch.xpack.support.clock.Clock;

/**
 *
 */
public class ActionThrottler implements Throttler {

    private static final AckThrottler ACK_THROTTLER = new AckThrottler();

    private final XPackLicenseState licenseState;
    private final PeriodThrottler periodThrottler;
    private final AckThrottler ackThrottler;

    public ActionThrottler(Clock clock, @Nullable TimeValue throttlePeriod, XPackLicenseState licenseState) {
        this(new PeriodThrottler(clock, throttlePeriod), ACK_THROTTLER, licenseState);
    }

    ActionThrottler(PeriodThrottler periodThrottler, AckThrottler ackThrottler, XPackLicenseState licenseState) {
        this.periodThrottler = periodThrottler;
        this.ackThrottler = ackThrottler;
        this.licenseState = licenseState;
    }

    public TimeValue throttlePeriod() {
        return periodThrottler != null ? periodThrottler.period() : null;
    }

    @Override
    public Result throttle(String actionId, WatchExecutionContext ctx) {
        if (licenseState.isWatcherAllowed() == false) {
            return Result.throttle("watcher license does not allow action execution");
        }
        if (periodThrottler != null) {
            Result throttleResult = periodThrottler.throttle(actionId, ctx);
            if (throttleResult.throttle()) {
                return throttleResult;
            }
        }
        return ackThrottler.throttle(actionId, ctx);
    }
}
