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

package org.elasticsearch.xpack.watcher.execution;

import java.util.Locale;

public enum ExecutionState {

    // the condition of the watch was not met
    EXECUTION_NOT_NEEDED,

    // Execution has been throttled due to ack/time-based throttling
    THROTTLED,

    // regular execution
    EXECUTED,

    // an error in the condition or the execution of the input
    FAILED,

    // the execution was scheduled, but in between the watch was deleted
    NOT_EXECUTED_WATCH_MISSING,

    // even though the execution was scheduled, it was not executed, because the watch was already queued in the thread pool
    NOT_EXECUTED_ALREADY_QUEUED,

    // this can happen when a watch was executed, but not completely finished (the triggered watch entry was not deleted), and then
    // watcher is restarted (manually or due to host switch) - the triggered watch will be executed but the history entry already
    // exists
    EXECUTED_MULTIPLE_TIMES;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ExecutionState resolve(String id) {
        return valueOf(id.toUpperCase(Locale.ROOT));
    }

    @Override
    public String toString() {
        return id();
    }

}
