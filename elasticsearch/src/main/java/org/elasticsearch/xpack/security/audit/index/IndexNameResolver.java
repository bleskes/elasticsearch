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

package org.elasticsearch.xpack.security.audit.index;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class IndexNameResolver {

    public enum Rollover {
        HOURLY  ("-yyyy.MM.dd.HH"),
        DAILY   ("-yyyy.MM.dd"),
        WEEKLY  ("-yyyy.w"),
        MONTHLY ("-yyyy.MM");

        private final DateTimeFormatter formatter;

        Rollover(String format) {
            this.formatter = DateTimeFormat.forPattern(format);
        }

        DateTimeFormatter formatter() {
            return formatter;
        }
    }

    private IndexNameResolver() {}

    public static String resolve(DateTime timestamp, Rollover rollover) {
        return rollover.formatter().print(timestamp);
    }

    public static String resolve(String indexNamePrefix, DateTime timestamp, Rollover rollover) {
        return indexNamePrefix + resolve(timestamp, rollover);
    }
}
