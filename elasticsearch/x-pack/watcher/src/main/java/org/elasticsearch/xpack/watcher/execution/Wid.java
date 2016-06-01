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

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import static org.elasticsearch.xpack.support.Exceptions.illegalArgument;

/**
 *
 */
public class Wid {

    private static final DateTimeFormatter formatter = ISODateTimeFormat.dateTime();

    private final String watchId;

    private final String value;

    public Wid(String watchId, long nonce, DateTime executionTime) {
        this.watchId = watchId;
        this.value = watchId + "_" + String.valueOf(nonce) + "-" +  formatter.print(executionTime);
    }

    public Wid(String value) {
        this.value = value;
        int index = value.lastIndexOf("_");
        if (index <= 0) {
            throw illegalArgument("invalid watcher execution id [{}]", value);
        }
        this.watchId = value.substring(0, index);
    }

    public String value() {
        return value;
    }

    public String watchId() {
        return watchId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Wid wid = (Wid) o;

        return value.equals(wid.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
