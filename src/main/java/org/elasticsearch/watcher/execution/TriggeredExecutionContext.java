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

import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.watcher.trigger.TriggerEvent;
import org.elasticsearch.watcher.watch.Watch;

/**
 */
public class TriggeredExecutionContext extends WatchExecutionContext {

    public TriggeredExecutionContext(Watch watch, DateTime executionTime, TriggerEvent triggerEvent) {
        super(watch, executionTime, triggerEvent);
    }

    @Override
    final public boolean simulateAction(String actionId) {
        return false;
    }

    @Override
    final public boolean recordInHistory() {
        return true;
    }

}
