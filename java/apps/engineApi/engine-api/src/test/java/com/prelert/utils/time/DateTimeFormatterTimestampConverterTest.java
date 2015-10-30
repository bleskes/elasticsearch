/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/

package com.prelert.utils.time;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

import org.junit.Test;

public class DateTimeFormatterTimestampConverterTest
{
    @Test (expected = IllegalArgumentException.class)
    public void testOfPattern_GivenPatternIsOnlyYear()
    {
        DateTimeFormatterTimestampConverter.ofPattern("y");
    }

    @Test (expected = IllegalArgumentException.class)
    public void testOfPattern_GivenPatternIsOnlyDate()
    {
        DateTimeFormatterTimestampConverter.ofPattern("y-M-d");
    }

    @Test (expected = IllegalArgumentException.class)
    public void testOfPattern_GivenPatternIsOnlyTime()
    {
        DateTimeFormatterTimestampConverter.ofPattern("HH:mm:ss");
    }

    @Test (expected = IllegalArgumentException.class)
    public void testOfPattern_GivenPatternIsUsingYearInsteadOfYearOfEra()
    {
        DateTimeFormatterTimestampConverter.ofPattern("uuuu-MM-dd HH:mm:ss");
    }

    @Test (expected = DateTimeParseException.class)
    public void testToEpochSeconds_GivenValidTimestampDoesNotFollowPattern()
    {
        TimestampConverter formatter = DateTimeFormatterTimestampConverter
                .ofPattern("yyyy-MM-dd HH:mm:ss");
        formatter.toEpochSeconds("14:00:22");
    }

    @Test (expected = DateTimeParseException.class)
    public void testToEpochMillis_GivenValidTimestampDoesNotFollowPattern()
    {
        TimestampConverter formatter = DateTimeFormatterTimestampConverter
                .ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        formatter.toEpochMillis("2015-01-01 14:00:22");
    }

    @Test
    public void testToEpochSeconds_GivenPatternHasFullDateAndOnlyHours()
    {
        long expected = ZonedDateTime.of(2014, 3, 22, 1, 0, 0, 0, ZoneOffset.systemDefault())
                .toEpochSecond();
        assertEquals(expected, toEpochSeconds("2014-03-22 01", "y-M-d HH"));
    }

    @Test
    public void testToEpochSeconds_GivenPatternHasFullDateAndTimeWithoutTimeZone()
    {
        long expected = ZonedDateTime.of(1985, 2, 18, 20, 15, 40, 0, ZoneOffset.systemDefault())
                .toEpochSecond();
        assertEquals(expected, toEpochSeconds("1985-02-18 20:15:40", "yyyy-MM-dd HH:mm:ss"));
    }

    @Test
    public void testToEpochSeconds_GivenPatternHasFullDateAndTimeWithTimeZone()
    {
        assertEquals(1395703820, toEpochSeconds("2014-03-25 01:30:20 +02:00", "yyyy-MM-dd HH:mm:ss XXX"));
    }

    @Test
    public void testToEpochSeconds_GivenPatternHasDateWithoutYearAndTimeWithoutTimeZone() throws ParseException
    {
        // Summertime
        long expected = ZonedDateTime.of(LocalDate.now().getYear(), 8, 14, 1, 30, 20, 0,
                ZoneOffset.systemDefault()).toEpochSecond();
        assertEquals(expected, toEpochSeconds("August 14 01:30:20", "MMMM dd HH:mm:ss"));

        // Non-summertime
        expected = ZonedDateTime.of(LocalDate.now().getYear(), 12, 14, 1, 30, 20, 0,
                ZoneOffset.systemDefault()).toEpochSecond();
        assertEquals(expected, toEpochSeconds("December 14 01:30:20", "MMMM dd HH:mm:ss"));
    }

    @Test
    public void testToEpochMillis_GivenPatternHasFullDateAndTimeWithTimeZone()
    {
        assertEquals(1395703820542L,
                toEpochMillis("2014-03-25 01:30:20.542 +02:00", "yyyy-MM-dd HH:mm:ss.SSS XXX"));
    }

    private static long toEpochSeconds(String timestamp, String pattern)
    {
        TimestampConverter formatter = DateTimeFormatterTimestampConverter.ofPattern(pattern);
        return formatter.toEpochSeconds(timestamp);
    }

    private static long toEpochMillis(String timestamp, String pattern)
    {
        TimestampConverter formatter = DateTimeFormatterTimestampConverter.ofPattern(pattern);
        return formatter.toEpochMillis(timestamp);
    }
}
