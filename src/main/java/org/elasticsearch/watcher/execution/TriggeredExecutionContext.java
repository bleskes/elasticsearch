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

package org.elasticsearch.watcher.execution;

import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.watcher.trigger.TriggerEvent;
import org.elasticsearch.watcher.watch.Watch;
import org.joda.time.DateTime;

/**
 */
public class TriggeredExecutionContext extends WatchExecutionContext {

    public TriggeredExecutionContext(Watch watch, DateTime executionTime, TriggerEvent triggerEvent, TimeValue defaultThrottlePeriod) {
        super(watch, executionTime, triggerEvent, defaultThrottlePeriod);
    }

    @Override
    public boolean knownWatch() {
        return true;
    }

    @Override
    public final boolean simulateAction(String actionId) {
        return false;
    }

    @Override
    public final boolean skipThrottling(String actionId) {
        return false;
    }

    @Override
    public final boolean recordExecution() {
        return true;
    }
}
