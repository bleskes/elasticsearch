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
 * A clock that can be modified for testing.
 */
public class ClockMock implements Clock {

    private volatile DateTime frozenNow;

    public ClockMock() {
        frozenNow = null;
    }

    /**
     * a utility method to create a new {@link ClockMock} and immediately call its {@link #freeze()} method
     */
    public static ClockMock frozen() {
        return new ClockMock().freeze();
    }


    @Override
    public long millis() {
        return nowUTC().getMillis();
    }

    @Override
    public long nanos() {
        return TimeUnit.MILLISECONDS.toNanos(millis());
    }

    @Override
    public DateTime nowUTC() {
        DateTime now = this.frozenNow;
        return now == null ?  DateTime.now(DateTimeZone.UTC) : now;
    }

    @Override
    public DateTime now(DateTimeZone timeZone) {
        return nowUTC().toDateTime(timeZone);
    }

    @Override
    public TimeValue timeElapsedSince(DateTime time) {
        return TimeValue.timeValueMillis(millis() - time.getMillis());
    }


    public synchronized ClockMock setTime(DateTime now) {
        this.frozenNow = now.toDateTime(DateTimeZone.UTC);
        return this;
    }

    /** freeze the time for this clock, preventing it from advancing */
    public synchronized ClockMock freeze() {
        setTime(nowUTC());
        return this;
    }

    /** the clock will be reset to current time and will advance from now */
    public synchronized ClockMock unfreeze() {
        this.frozenNow = null;
        return this;
    }

    public ClockMock fastForward(TimeValue timeValue) {
        return setTime(nowUTC().plusMillis((int) timeValue.millis()));
    }

    public ClockMock fastForwardSeconds(int seconds) {
        return fastForward(TimeValue.timeValueSeconds(seconds));
    }

    public ClockMock rewind(TimeValue timeValue) {
        return setTime(nowUTC().minusMillis((int) timeValue.millis()));
    }

    public ClockMock rewindSeconds(int seconds) {
        return rewind(TimeValue.timeValueSeconds(seconds));
    }
}
