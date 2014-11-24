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

package org.elasticsearch.alerts;

import org.elasticsearch.ElasticsearchIllegalArgumentException;

/**
 * Encapsulates the state of the alerts plugin.
 */
public enum State {

    /**
     * The alerts plugin is not running and not functional.
     */
    STOPPED(0),

    /**
     * The alerts plugin is performing the necessary operations to get into a started state.
     */
    STARTING(1),

    /**
     * The alerts plugin is running and completely functional.
     */
    STARTED(2),

    /**
     * The alerts plugin is shutting down and not functional.
     */
    STOPPING(3);

    private final byte id;

    State(int id) {
        this.id = (byte) id;
    }

    public byte getId() {
        return id;
    }

    public static State fromId(byte id) {
        switch (id) {
            case 0:
                return STOPPED;
            case 1:
                return STARTING;
            case 2:
                return STARTED;
            case 3:
                return STOPPING;
            default:
                throw new ElasticsearchIllegalArgumentException("Unknown id: " + id);
        }
    }
}
