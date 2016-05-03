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

import org.elasticsearch.xpack.watcher.actions.ActionStatus;
import org.elasticsearch.xpack.watcher.actions.ActionStatus.AckStatus;
import org.elasticsearch.xpack.watcher.execution.WatchExecutionContext;

import static org.elasticsearch.xpack.watcher.support.WatcherDateTimeUtils.formatDate;

/**
 *
 */
public class AckThrottler implements Throttler {

    @Override
    public Result throttle(String actionId, WatchExecutionContext ctx) {
        ActionStatus actionStatus = ctx.watch().status().actionStatus(actionId);
        AckStatus ackStatus = actionStatus.ackStatus();
        if (ackStatus.state() == AckStatus.State.ACKED) {
            return Result.throttle("action [{}] was acked at [{}]", actionId, formatDate(ackStatus.timestamp()));
        }
        return Result.NO;
    }
}
