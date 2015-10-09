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

package org.elasticsearch.marvel.mode;

import org.elasticsearch.ElasticsearchException;

import java.util.Locale;

/**
 * Marvel's operating mode
 */
public enum Mode {

    /**
     * Marvel runs in downgraded mode
     *
     * TODO: do we really need mode?
     */
    TRIAL(0),

    /**
     * Marvel runs in downgraded mode
     */
    LITE(0),

    /**
     * Marvel runs in normal mode
     */
    STANDARD(1);

    private final byte id;

    Mode(int id) {
        this.id = (byte) id;
    }

    public byte getId() {
        return id;
    }

    public static Mode fromId(byte id) {
        switch (id) {
            case 0:
                return TRIAL;
            case 1:
                return LITE;
            case 2:
                return STANDARD;
            case 3:
            default:
                throw new ElasticsearchException("unknown marvel mode id [" + id + "]");
        }
    }

    public static Mode fromName(String name) {
        switch (name.toLowerCase(Locale.ROOT)) {
            case "trial":
                return LITE;
            case "basic":
            case "gold" :
            case "silver":
            case "platinum":
                return STANDARD;
            default:
                throw new ElasticsearchException("unknown marvel mode name [" + name + "]");
        }
    }
}
