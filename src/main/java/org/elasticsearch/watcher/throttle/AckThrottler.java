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

import org.elasticsearch.watcher.execution.WatchExecutionContext;

import static org.elasticsearch.watcher.support.WatcherDateUtils.formatDate;

/**
 *
 */
public class AckThrottler implements Throttler {

    @Override
    public Result throttle(WatchExecutionContext ctx) {
        if (ctx.watch().acked()) {
            return Result.throttle("watch [" + ctx.watch().name() + "] was acked at [" + formatDate(ctx.watch().status().ackStatus().timestamp()) + "]");
        }
        return Result.NO;
    }
}
