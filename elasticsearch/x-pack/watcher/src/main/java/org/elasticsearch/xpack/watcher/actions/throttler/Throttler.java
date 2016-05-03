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

import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.logging.LoggerMessageFormat;
import org.elasticsearch.xpack.watcher.execution.WatchExecutionContext;

/**
 *
 */
public interface Throttler {

    Result throttle(String actionId, WatchExecutionContext ctx);

    class Result {

        public static final Result NO = new Result(false, null);
        
        private final boolean throttle;
        private final String reason;

        private Result(boolean throttle, String reason) {
            this.throttle = throttle;
            this.reason = reason;
        }

        public static Result throttle(String reason, Object... args) {
            return new Result(true, LoggerMessageFormat.format(reason, args));
        }

        public boolean throttle() {
            return throttle;
        }

        public String reason() {
            return reason;
        }

    }

    interface Field {
        ParseField THROTTLE_PERIOD = new ParseField("throttle_period");
    }
}
