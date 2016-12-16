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

/**
 */
public enum ExecutionPhase {

    // awaiting execution of the watch
    AWAITS_EXECUTION(false),
    // initial phase, watch execution has started, but the input is not yet processed
    STARTED(false),
    // input is being executed
    INPUT(false),
    // condition phase is being executed
    CONDITION(false),
    // transform phase (optional, depends if a global transform was configured in the watch)
    WATCH_TRANSFORM(false),
    // actions phase, all actions, including specific action transforms
    ACTIONS(false),
    // missing watch, failed execution of input/condition/transform,
    ABORTED(true),
    // successful run
    FINISHED(true);

    private final boolean sealed;

    ExecutionPhase(boolean sealed) {
        this.sealed = sealed;
    }

    public boolean sealed() {
        return sealed;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ExecutionPhase resolve(String id) {
        return valueOf(id.toUpperCase(Locale.ROOT));
    }

    @Override
    public String toString() {
        return id();
    }
}
