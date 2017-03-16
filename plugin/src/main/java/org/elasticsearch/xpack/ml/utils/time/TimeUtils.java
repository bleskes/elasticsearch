/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.ml.utils.time;

import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.mapper.DateFieldMapper;
import org.elasticsearch.xpack.ml.job.messages.Messages;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public final class TimeUtils {
    private TimeUtils() {
        // Do nothing
    }

    /**
     * First tries to parse the date first as a Long and convert that to an
     * epoch time. If the long number has more than 10 digits it is considered a
     * time in milliseconds else if 10 or less digits it is in seconds. If that
     * fails it tries to parse the string using
     * {@link DateFieldMapper#DEFAULT_DATE_TIME_FORMATTER}
     *
     * If the date string cannot be parsed -1 is returned.
     *
     * @return The epoch time in milliseconds or -1 if the date cannot be
     *         parsed.
     */
    public static long dateStringToEpoch(String date) {
        try {
            long epoch = Long.parseLong(date);
            if (date.trim().length() <= 10) { // seconds
                return epoch * 1000;
            } else {
                return epoch;
            }
        } catch (NumberFormatException nfe) {
            // not a number
        }

        try {
            return DateFieldMapper.DEFAULT_DATE_TIME_FORMATTER.parser().parseMillis(date);
        } catch (IllegalArgumentException e) {
        }
        // Could not do the conversion
        return -1;
    }

    /**
     * Checks that the given {@code timeValue} is a non-negative multiple value of the {@code baseUnit}.
     *
     * <ul>
     *   <li>400ms is valid for base unit of seconds</li>
     *   <li>450ms is invalid for base unit of seconds but valid for base unit of milliseconds</li>
     * </ul>
     */
    public static void checkNonNegativeMultiple(TimeValue timeValue, TimeUnit baseUnit, ParseField field) {
        checkNonNegative(timeValue, field);
        checkMultiple(timeValue, baseUnit, field);
    }

    /**
     * Checks that the given {@code timeValue} is a positive multiple value of the {@code baseUnit}.
     *
     * <ul>
     *   <li>400ms is valid for base unit of seconds</li>
     *   <li>450ms is invalid for base unit of seconds but valid for base unit of milliseconds</li>
     * </ul>
     */
    public static void checkPositiveMultiple(TimeValue timeValue, TimeUnit baseUnit, ParseField field) {
        checkPositive(timeValue, field);
        checkMultiple(timeValue, baseUnit, field);
    }

    private static void checkNonNegative(TimeValue timeValue, ParseField field) {
        long nanos = timeValue.getNanos();
        if (nanos < 0) {
            throw new IllegalArgumentException(field.getPreferredName() + " cannot be less than 0. Value = " + timeValue.toString());
        }
    }

    private static void checkPositive(TimeValue timeValue, ParseField field) {
        long nanos = timeValue.getNanos();
        if (nanos <= 0) {
            throw new IllegalArgumentException(field.getPreferredName() + " cannot be less or equal than 0. Value = "
                    + timeValue.toString());
        }
    }

    /**
     * Check the given {@code timeValue} is a multiple of the {@code baseUnit}
     */
    public static void checkMultiple(TimeValue timeValue, TimeUnit baseUnit, ParseField field) {
        long nanos = timeValue.getNanos();
        TimeValue base = new TimeValue(1, baseUnit);
        long baseNanos = base.getNanos();
        if (nanos % baseNanos != 0) {
            throw new IllegalArgumentException(field.getPreferredName() + " has to be a multiple of " + base.toString() + "; actual was '"
                    + timeValue.toString() + "'");
        }
    }
}
