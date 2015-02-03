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
import org.elasticsearch.alerts.trigger.Trigger;
import org.elasticsearch.common.collect.ImmutableList;

import java.util.List;

/**
 *
 */
public class CompoundThrottler implements Throttler {

    private final List<Throttler> throttlers;

    public CompoundThrottler(List<Throttler> throttlers) {
        this.throttlers = throttlers;
    }

    @Override
    public Result throttle(Alert alert, Trigger.Result result) {
        for (Throttler throttler : throttlers) {
            Result rslt = throttler.throttle(alert, result);
            if (rslt.throttle()) {
                return rslt;
            }
        }
        return Result.NO;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final ImmutableList.Builder<Throttler> throttlers = ImmutableList.builder();

        private Builder() {
        }

        public Builder add(Throttler throttler) {
            throttlers.add(throttler);
            return this;
        }

        public Throttler build() {
            ImmutableList<Throttler> list = throttlers.build();
            return list.isEmpty() ? Throttler.NO_THROTTLE : new CompoundThrottler(list);
        }
    }
}
