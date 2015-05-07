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

package org.elasticsearch.shield.audit.index;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 *
 */
public class IndexNameResolver {

    private final DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, Locale.ROOT);

    public enum Rollover {
        HOURLY  ("-yyyy-MM-dd-HH"),
        DAILY   ("-yyyy-MM-dd"),
        WEEKLY  ("-yyyy-w"),
        MONTHLY ("-yyyy-MM");

        private final String format;

        Rollover(String format) {
            this.format = format;
        }
    }

    public String resolve(long timestamp, Rollover rollover) {
        Date date = new Date(timestamp);
        ((SimpleDateFormat) formatter).applyPattern(rollover.format);
        return formatter.format(date);
    }

    public String resolve(String indexNamePrefix, long timestamp, Rollover rollover) {
        return indexNamePrefix + resolve(timestamp, rollover);
    }
}
