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

package org.elasticsearch.alerts.support;

import org.elasticsearch.alerts.AlertsSettingsException;
import org.elasticsearch.common.joda.FormatDateTimeFormatter;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.mapper.core.DateFieldMapper;

import java.io.IOException;

/**
 *
 */
public class AlertsDateUtils {

    public static final FormatDateTimeFormatter dateTimeFormatter = DateFieldMapper.Defaults.DATE_TIME_FORMATTER;

    private AlertsDateUtils() {
    }

    public static DateTime parseDate(String format) {
        return dateTimeFormatter.parser().parseDateTime(format);
    }

    public static String formatDate(DateTime date) {
        return dateTimeFormatter.printer().print(date);
    }

    public static DateTime parseDate(String fieldName, XContentParser.Token token, XContentParser parser) throws IOException {
        if (token == XContentParser.Token.VALUE_NUMBER) {
            return new DateTime(parser.longValue());
        }
        if (token == XContentParser.Token.VALUE_STRING) {
            return dateTimeFormatter.parser().parseDateTime(parser.text());
        }
        if (token == XContentParser.Token.VALUE_NULL) {
            return null;
        }
        throw new AlertsSettingsException("could not parse date/time. expected [" + fieldName +
                    "] to be either a number or a string but was [" + token + "] instead");
    }
}
