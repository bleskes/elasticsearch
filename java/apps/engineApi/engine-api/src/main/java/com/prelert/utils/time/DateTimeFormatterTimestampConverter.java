/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
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

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;

/**
 * <p>
 * This class implements {@link TimestampConverter} using the {@link DateTimeFormatter}
 * of the Java 8 time API for parsing timestamps and other classes of that API for converting
 * timestamps to epoch times.
 * </p>
 *
 * <p>
 * Objects of this class are <b>immutable</b> and <b>thread-safe</b>
 * </p>
 *
 */
public class DateTimeFormatterTimestampConverter implements TimestampConverter
{
    private final DateTimeFormatter m_Formatter;
    private final boolean m_HasTimeZone;
    private final ZoneId m_SystemDefaultZoneId;

    private DateTimeFormatterTimestampConverter(DateTimeFormatter dateTimeFormatter,
            boolean hasTimeZone)
    {
        m_Formatter = dateTimeFormatter;
        m_HasTimeZone = hasTimeZone;
        m_SystemDefaultZoneId = ZoneOffset.systemDefault();
    }

    /**
     * Creates a formatter according to the given pattern
     * @param pattern the pattern to be used by the formatter, not null.
     * See {@link java.time.format.DateTimeFormatter} for the syntax of the accepted patterns
     * @return a {@code TimestampConverter}
     * @throws IllegalArgumentException if the pattern is invalid or cannot produce a full timestamp
     * (e.g. contains a date but not a time)
     */
    public static TimestampConverter ofPattern(String pattern)
    {
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .parseLenient()
                .appendPattern(pattern)
                .parseDefaulting(ChronoField.YEAR_OF_ERA, LocalDate.now().getYear())
                .toFormatter();

        String now = formatter.format(ZonedDateTime.ofInstant(Instant.ofEpochSecond(0), ZoneOffset.UTC));
        try
        {
            TemporalAccessor parsed = formatter.parse(now);
            boolean hasTimeZone = parsed.isSupported(ChronoField.INSTANT_SECONDS);
            if (hasTimeZone)
            {
                Instant.from(parsed);
            }
            else
            {
                LocalDateTime.from(parsed);
            }
            return new DateTimeFormatterTimestampConverter(formatter, hasTimeZone);
        }
        catch (DateTimeException e)
        {
            throw new IllegalArgumentException("Timestamp cannot be derived from pattern: " + pattern);
        }
    }

    @Override
    public long toEpochSeconds(String timestamp)
    {
        return toInstant(timestamp).getEpochSecond();
    }

    @Override
    public long toEpochMillis(String timestamp)
    {
        return toInstant(timestamp).toEpochMilli();
    }

    private Instant toInstant(String timestamp)
    {
        TemporalAccessor parsed = m_Formatter.parse(timestamp);
        if (m_HasTimeZone)
        {
            return Instant.from(parsed);
        }
        return LocalDateTime.from(parsed).atZone(m_SystemDefaultZoneId).toInstant();
    }
}
