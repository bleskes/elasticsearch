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

package org.elasticsearch.marvel;

import java.util.Locale;

public enum MonitoringIds {

    ES("es");

    private final String id;

    MonitoringIds(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static MonitoringIds fromId(String id) {
        switch (id.toLowerCase(Locale.ROOT)) {
            case "es":
                return ES;
            default:
                throw new IllegalArgumentException("Unknown monitoring id [" + id + "]");
        }
    }
}
