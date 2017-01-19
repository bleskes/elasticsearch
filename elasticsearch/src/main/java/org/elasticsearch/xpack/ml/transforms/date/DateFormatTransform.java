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
package org.elasticsearch.xpack.ml.transforms.date;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.xpack.ml.transforms.TransformException;
import org.elasticsearch.xpack.ml.utils.time.DateTimeFormatterTimestampConverter;
import org.elasticsearch.xpack.ml.utils.time.TimestampConverter;

import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

/**
 * A transform that attempts to parse a String timestamp
 * according to a timeFormat. It converts that
 * to a long that represents the equivalent epoch.
 */
public class DateFormatTransform extends DateTransform {
    private final String timeFormat;
    private final TimestampConverter dateToEpochConverter;

    public DateFormatTransform(String timeFormat, List<TransformIndex> readIndexes, List<TransformIndex> writeIndexes, Logger logger) {
        super(readIndexes, writeIndexes, logger);

        this.timeFormat = timeFormat;
        dateToEpochConverter = DateTimeFormatterTimestampConverter.ofPattern(timeFormat, ZoneOffset.UTC);
    }

    @Override
    protected long toEpochMs(String field) throws TransformException {
        try {
            return dateToEpochConverter.toEpochMillis(field);
        } catch (DateTimeParseException pe) {
            String message = String.format(Locale.ROOT, "Cannot parse date '%s' with format string '%s'", field, timeFormat);
            throw new ParseTimestampException(message);
        }
    }
}