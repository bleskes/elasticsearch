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

package org.elasticsearch.watcher.trigger.schedule;

import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class IntervalSchedule implements Schedule {

    public static final String TYPE = "interval";

    private final Interval interval;

    public IntervalSchedule(Interval interval) {
        if (interval.millis < 1000) {
            throw new ScheduleTriggerException("interval can't be lower than 1000 ms, but [{}] was specified", interval);
        }
        this.interval = interval;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public long nextScheduledTimeAfter(long startTime, long time) {
        assert time >= startTime;
        if (startTime == time) {
            time++;
        }
        long delta = time - startTime;
        return startTime + (delta / interval.millis + 1) * interval.millis;
    }

    public Interval interval() {
        return interval;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return interval.toXContent(builder, params);
    }

    @Override
    public String toString() {
        return interval.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IntervalSchedule schedule = (IntervalSchedule) o;

        if (!interval.equals(schedule.interval)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return interval.hashCode();
    }

    public static class Parser implements Schedule.Parser<IntervalSchedule> {

        @Override
        public String type() {
            return TYPE;
        }

        @Override
        public IntervalSchedule parse(XContentParser parser) throws IOException {
            XContentParser.Token token = parser.currentToken();
            if (token == XContentParser.Token.VALUE_NUMBER) {
                return new IntervalSchedule(Interval.seconds(parser.longValue()));
            }
            if (token == XContentParser.Token.VALUE_STRING) {
                String value = parser.text();
                return new IntervalSchedule(Interval.parse(value));
            }
            throw new ScheduleTriggerException("could not parse [interval] schedule. expected either a numeric value " +
                    "(millis) or a string value representing time value (e.g. '5s'), but found [" + token + "]");
        }
    }

    /**
     * Represents a time interval. Ideally we would have used TimeValue here, but we don't because:
     * 1. We should limit the time values that the user can configure (we don't want to support nanos & millis
     * 2. TimeValue formatting & parsing is inconsistent (it doesn't format to a value that it can parse)
     * 3. The equals of TimeValue is odd - it will only equate two time values that have the exact same unit & duration,
     *    this interval on the other hand, equates based on the millis value.
     * 4. We have the advantage of making this interval construct a ToXContent
     */
    public static class Interval implements ToXContent {

        public static enum Unit {
            SECONDS(TimeUnit.SECONDS.toMillis(1), "s"),
            MINUTES(TimeUnit.MINUTES.toMillis(1), "m"),
            HOURS(TimeUnit.HOURS.toMillis(1), "h"),
            DAYS(TimeUnit.DAYS.toMillis(1), "d"),
            WEEK(TimeUnit.DAYS.toMillis(7), "w");

            private final String suffix;
            private final long millis;

            private Unit(long millis, String suffix) {
                this.millis = millis;
                this.suffix = suffix;
            }

            public long millis(long duration) {
                return duration * millis;
            }

            public long parse(String value) {
                assert value.endsWith(suffix);
                String num = value.substring(0, value.indexOf(suffix));
                try {
                    return Long.parseLong(num);
                } catch (NumberFormatException nfe) {
                    throw new ScheduleTriggerException("could not parse [interval] schedule. could not parse ["
                            + num + "] as a " + name().toLowerCase(Locale.ROOT) + " duration");
                }
            }

            public String format(long duration) {
                return duration + suffix;
            }
        }

        private final long duration;
        private final Unit unit;
        private final long millis; // computed once

        public Interval(long duration, Unit unit) {
            this.duration = duration;
            this.unit = unit;
            this.millis = unit.millis(duration);
        }

        public long seconds() {
            return unit.millis(duration) / Unit.SECONDS.millis;
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            return builder.value(unit.format(duration));
        }

        @Override
        public String toString() {
            return unit.format(duration);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Interval interval = (Interval) o;

            if (unit.millis(duration) != interval.unit.millis(interval.duration)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            long millis = unit.millis(duration);
            int result = (int) (millis ^ (millis >>> 32));
            return result;
        }

        public static Interval seconds(long duration) {
            return new Interval(duration, Unit.SECONDS);
        }

        public static Interval parse(String value) {
            for (Unit unit : Unit.values()) {
                if (value.endsWith(unit.suffix)) {
                    return new Interval(unit.parse(value), unit);
                }
            }
            throw new ScheduleTriggerException("could not parse [interval] schedule. unrecognized interval format [" + value + "]");
        }
    }
}
