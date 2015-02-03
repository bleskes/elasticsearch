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

/**
 *
 */
public interface Throttler {

    public static final Throttler NO_THROTTLE = new Throttler() {
        @Override
        public Result throttle(Alert Alert, Trigger.Result result) {
            return Result.NO;
        }
    };

    Result throttle(Alert alert, Trigger.Result result);

    static class Result {

        static final Result NO = new Result(false, null);
        
        private final boolean throttle;
        private final String reason;

        private Result(boolean throttle, String reason) {
            this.throttle = throttle;
            this.reason = reason;
        }

        public static Result throttle(String reason) {
            return new Result(true, reason);
        }

        public boolean throttle() {
            return throttle;
        }

        public String reason() {
            return reason;
        }
    }
}
