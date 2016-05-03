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

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

public final class TimeUtils
{
    /**
     * A {@code DateTimeFormatter} for ISO 8601. It accepts offsets like:
     * <ul>
     * <li> +00:00, + 02:30
     * <li> +0000, +0230
     * <li> +00, +02
     * <li> Z
     * </ul>
     */
    private static final DateTimeFormatter ISO_8601_DATE_PARSER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSS][XXX][X]");

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

    /**
     * Parses a {@code String} expected to be formatted as an ISO 8601 timestamp.
     * Supported offsets include:
     * <ul>
     * <li> +00:00, + 02:30
     * <li> +0000, +0230
     * <li> +00, +02
     * <li> Z
     * </ul>
     * @param timestamp a {@code String} containing an ISO 8601 timestamp
     * @return the epoch milliseconds
     */
    public static long parseIso8601AsEpochMillis(String timestamp)
    {
        TemporalAccessor parsed = ISO_8601_DATE_PARSER.parse(timestamp);
        return Instant.from(parsed).toEpochMilli();
    }
}
