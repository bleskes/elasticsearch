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

package org.elasticsearch.xpack.watcher.support.clock;

import org.elasticsearch.common.unit.TimeValue;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 *
 */
public class HaltedClock implements Clock {

    private final DateTime now;

    public HaltedClock(DateTime now) {
        this.now = now.toDateTime(DateTimeZone.UTC);
    }

    @Override
    public long millis() {
        return now.getMillis();
    }

    @Override
    public long nanos() {
        return millis() * 1000000;
    }

    @Override
    public DateTime nowUTC() {
        return now;
    }

    @Override
    public DateTime now(DateTimeZone timeZone) {
        return now.toDateTime(timeZone);
    }

    @Override
    public TimeValue timeElapsedSince(DateTime time) {
        return TimeValue.timeValueMillis(millis() - time.getMillis());
    }
}
