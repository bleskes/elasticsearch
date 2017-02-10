/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2017 Elasticsearch BV. All Rights Reserved.
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
package org.elasticsearch.xpack.ml.job.process.autodetect.writer;

import org.elasticsearch.xpack.ml.utils.time.DateTimeFormatterTimestampConverter;
import org.elasticsearch.xpack.ml.utils.time.TimestampConverter;

import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * A transformer that attempts to parse a String timestamp as a data according to a time format.
 * It converts that to a long that represents the equivalent milliseconds since the epoch.
 */
public class DateFormatDateTransformer implements DateTransformer {

    private final String timeFormat;
    private final TimestampConverter dateToEpochConverter;

    public DateFormatDateTransformer(String timeFormat) {
        this.timeFormat = timeFormat;
        dateToEpochConverter = DateTimeFormatterTimestampConverter.ofPattern(timeFormat, ZoneOffset.UTC);
    }

    @Override
    public long transform(String timestamp) throws CannotParseTimestampException {
        try {
            return dateToEpochConverter.toEpochMillis(timestamp);
        } catch (DateTimeParseException e) {
            String message = String.format(Locale.ROOT, "Cannot parse date '%s' with format string '%s'", timestamp, timeFormat);
            throw new CannotParseTimestampException(message, e);
        }
    }
}
