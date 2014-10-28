/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
