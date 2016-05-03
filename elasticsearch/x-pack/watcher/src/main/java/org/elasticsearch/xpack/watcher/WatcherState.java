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

package org.elasticsearch.xpack.watcher;

/**
 * Encapsulates the state of the watcher plugin.
 */
public enum WatcherState {

    /**
     * The watcher plugin is not running and not functional.
     */
    STOPPED(0),

    /**
     * The watcher plugin is performing the necessary operations to get into a started state.
     */
    STARTING(1),

    /**
     * The watcher plugin is running and completely functional.
     */
    STARTED(2),

    /**
     * The watcher plugin is shutting down and not functional.
     */
    STOPPING(3);

    private final byte id;

    WatcherState(int id) {
        this.id = (byte) id;
    }

    public byte getId() {
        return id;
    }

    public static WatcherState fromId(byte id) {
        switch (id) {
            case 0:
                return STOPPED;
            case 1:
                return STARTING;
            case 2:
                return STARTED;
            default: //3
                assert id == 3 : "unknown watcher state id [" + id + "]";
                return STOPPING;
        }
    }
}
