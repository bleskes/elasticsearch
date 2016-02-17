package com.prelert.utils.time;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class TimeUtils
{
    private TimeUtils()
    {
        // Do nothing
    }

    public static String formatEpochMillisAsIso(long epochMillis)
    {
        Instant instant = Instant.ofEpochMilli(epochMillis);
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(zonedDateTime);
    }
}
