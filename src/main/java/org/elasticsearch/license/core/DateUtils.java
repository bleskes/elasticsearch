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
import org.elasticsearch.common.joda.time.MutableDateTime;
import org.elasticsearch.common.joda.time.format.DateTimeFormatter;

public class DateUtils {

    private final static FormatDateTimeFormatter formatDateTimeFormatter = Joda.forPattern("yyyy-MM-dd");

    private final static DateTimeFormatter dateTimeFormatter = formatDateTimeFormatter.parser().withZoneUTC();

    public static long endOfTheDay(String date) {
        MutableDateTime dateTime = dateTimeFormatter.parseMutableDateTime(date);
        dateTime.dayOfMonth().roundCeiling();
        return dateTime.getMillis();
    }

    public static long beginningOfTheDay(String date) {
        return dateTimeFormatter.parseDateTime(date).getMillis();
    }
}
