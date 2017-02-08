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

import java.time.format.DateTimeParseException;

/**
 * A converter that enables conversions of textual timestamps to epoch seconds
 * or milliseconds according to a given pattern.
 */
public interface TimestampConverter {
    /**
     * Converts the a textual timestamp into an epoch in seconds
     *
     * @param timestamp the timestamp to convert, not null. The timestamp is expected to
     * be formatted according to the pattern of the formatter. In addition, the pattern is
     * assumed to contain both date and time information.
     * @return the epoch in seconds for the given timestamp
     * @throws DateTimeParseException if unable to parse the given timestamp
     */
    long toEpochSeconds(String timestamp);

    /**
     * Converts the a textual timestamp into an epoch in milliseconds
     *
     * @param timestamp the timestamp to convert, not null. The timestamp is expected to
     * be formatted according to the pattern of the formatter. In addition, the pattern is
     * assumed to contain both date and time information.
     * @return the epoch in milliseconds for the given timestamp
     * @throws DateTimeParseException if unable to parse the given timestamp
     */
    long toEpochMillis(String timestamp);
}
