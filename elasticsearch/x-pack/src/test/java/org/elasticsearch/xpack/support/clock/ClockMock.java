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

package org.elasticsearch.xpack.support.clock;

import org.elasticsearch.common.unit.TimeValue;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.concurrent.TimeUnit;

/**
 *
 */
public class ClockMock implements Clock {

    private DateTime now = DateTime.now(DateTimeZone.UTC);

    @Override
    public long millis() {
        return now.getMillis();
    }

    @Override
    public long nanos() {
        return TimeUnit.MILLISECONDS.toNanos(now.getMillis());
    }

    @Override
    public DateTime nowUTC() {
        return now(DateTimeZone.UTC);
    }

    @Override
    public DateTime now(DateTimeZone timeZone) {
        return now.toDateTime(timeZone);
    }

    @Override
    public TimeValue timeElapsedSince(DateTime time) {
        return TimeValue.timeValueMillis(now.getMillis() - time.getMillis());
    }

    public ClockMock setTime(DateTime now) {
        this.now = now;
        return this;
    }

    public ClockMock fastForward(TimeValue timeValue) {
        return setTime(now.plusMillis((int) timeValue.millis()));
    }

    public ClockMock fastForwardSeconds(int seconds) {
        return fastForward(TimeValue.timeValueSeconds(seconds));
    }

    public ClockMock rewind(TimeValue timeValue) {
        return setTime(now.minusMillis((int) timeValue.millis()));
    }

    public ClockMock rewindSeconds(int seconds) {
        return rewind(TimeValue.timeValueSeconds(seconds));
    }
}
