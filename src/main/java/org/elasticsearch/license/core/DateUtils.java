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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateUtils {
    public static final DateFormat DATE_FORMAT;
    public static final TimeZone TIME_ZONE = TimeZone.getTimeZone("UTC");

    static {
        DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
        DATE_FORMAT.setTimeZone(DateUtils.TIME_ZONE);
        DATE_FORMAT.setLenient(false);
    }

    public static long longExpiryDateFromDate(long date) {
        Date dateObj = new Date(date);

        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.setTimeZone(TIME_ZONE);
        calendar.setTimeInMillis(dateObj.getTime());

        calendar.set(Calendar.HOUR, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);

        return calendar.getTimeInMillis();
    }

    public static long longFromDateString(String dateStr) throws ParseException {
        Date dateObj = DATE_FORMAT.parse(dateStr);
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.setTimeZone(TIME_ZONE);
        calendar.setTimeInMillis(dateObj.getTime());
        return calendar.getTimeInMillis();
    }

    public static long longExpiryDateFromString(String dateStr) throws ParseException {
        return longExpiryDateFromDate(longFromDateString(dateStr));
    }

    public static String dateStringFromLongDate(long date) {
        Date dateObj = new Date(date);
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.setTimeZone(TIME_ZONE);
        calendar.setTimeInMillis(dateObj.getTime());
        return DATE_FORMAT.format(calendar.getTime());
    }
}
