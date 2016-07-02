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

package org.elasticsearch.license.core;

import org.elasticsearch.common.joda.FormatDateTimeFormatter;
import org.elasticsearch.common.joda.Joda;
import org.joda.time.MutableDateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class DateUtils {

    private static final FormatDateTimeFormatter formatDateOnlyFormatter = Joda.forPattern("yyyy-MM-dd");

    private static final DateTimeFormatter dateOnlyFormatter = formatDateOnlyFormatter.parser().withZoneUTC();

    private static final DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTime().withZoneUTC();

    public static long endOfTheDay(String date) {
        try {
            // Try parsing using complete date/time format
            return dateTimeFormatter.parseDateTime(date).getMillis();
        } catch (IllegalArgumentException ex) {
            // Fall back to the date only format
            MutableDateTime dateTime = dateOnlyFormatter.parseMutableDateTime(date);
            dateTime.millisOfDay().set(dateTime.millisOfDay().getMaximumValue());
            return dateTime.getMillis();
        }
    }

    public static long beginningOfTheDay(String date) {
        try {
            // Try parsing using complete date/time format
            return dateTimeFormatter.parseDateTime(date).getMillis();
        } catch (IllegalArgumentException ex) {
            // Fall back to the date only format
            return dateOnlyFormatter.parseDateTime(date).getMillis();
        }

    }
}
