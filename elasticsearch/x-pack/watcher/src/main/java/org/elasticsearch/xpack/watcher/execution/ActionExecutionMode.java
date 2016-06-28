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

import static org.elasticsearch.xpack.watcher.support.Exceptions.illegalArgument;

/**
 *
 */
public enum ActionExecutionMode {

    /**
     * The action will be simulated (not actually executed) and it will be throttled if needed.
     */
    SIMULATE((byte) 1, false, true),

    /**
     * The action will be simulated (not actually executed) and it will <b>not</b> be throttled.
     */
    FORCE_SIMULATE((byte) 2, true, true),

    /**
     * The action will be executed and it will be throttled if needed.
     */
    EXECUTE((byte) 3, false, false),

    /**
     * The action will be executed and it will <b>not</b> be throttled.
     */
    FORCE_EXECUTE((byte) 4, true, false),

    /**
     * The action will be skipped (it won't be executed nor simulated) - effectively it will be forcefully throttled
     */
    SKIP((byte) 5, false, false);

    private final byte id;
    private final boolean force;
    private final boolean simulate;

    ActionExecutionMode(byte id, boolean froce, boolean simulate) {
        this.id = id;
        this.force = froce;
        this.simulate = simulate;
    }

    public final byte id() {
        return id;
    }

    public final boolean simulate() {
        return simulate;
    }

    public final boolean force() {
        return force;
    }

    public static ActionExecutionMode resolve(byte id) {
        switch (id) {
            case 1: return SIMULATE;
            case 2: return FORCE_SIMULATE;
            case 3: return EXECUTE;
            case 4: return FORCE_EXECUTE;
            case 5: return SKIP;
        }
        throw illegalArgument("unknown action execution mode id [{}]", id);
    }

    public static ActionExecutionMode resolve(String key) {
        if (key == null) {
            return null;
        }
        switch (key.toLowerCase(Locale.ROOT)) {
            case "simulate":        return SIMULATE;
            case "force_simulate":  return FORCE_SIMULATE;
            case "execute":         return EXECUTE;
            case "force_execute":   return FORCE_EXECUTE;
            case "skip":            return SKIP;
        }
        throw illegalArgument("unknown action execution mode [{}]", key);
    }
}
