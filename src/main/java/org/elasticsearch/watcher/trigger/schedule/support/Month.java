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

package org.elasticsearch.watcher.trigger.schedule.support;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Locale;

/**
 *
 */
public enum Month implements ToXContent {

    JANUARY("JAN"),
    FEBRUARY("FEB"),
    MARCH("MAR"),
    APRIL("APR"),
    MAY("MAY"),
    JUNE("JUN"),
    JULY("JUL"),
    AUGUST("AUG"),
    SEPTEMBER("SEP"),
    OCTOBER("OCT"),
    NOVEMBER("NOV"),
    DECEMBER("DEC");

    private final String cronKey;

    private Month(String cronKey) {
        this.cronKey = cronKey;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.value(name().toLowerCase(Locale.ROOT));
    }

    public static String cronPart(EnumSet<Month> days) {
        StringBuilder sb = new StringBuilder();
        for (Month day : days) {
            if (sb.length() != 0) {
                sb.append(",");
            }
            sb.append(day.cronKey);
        }
        return sb.toString();
    }

    public static Month resolve(int month) {
        switch (month) {
            case 1: return JANUARY;
            case 2: return FEBRUARY;
            case 3: return MARCH;
            case 4: return APRIL;
            case 5: return MAY;
            case 6: return JUNE;
            case 7: return JULY;
            case 8: return AUGUST;
            case 9: return SEPTEMBER;
            case 10: return OCTOBER;
            case 11: return NOVEMBER;
            case 12: return DECEMBER;
            default:
                throw new ElasticsearchParseException("unknown month number [{}]", month);
        }
    }

    public static Month resolve(String day) {
        switch (day.toLowerCase(Locale.ROOT)) {
            case "1":
            case "jan":
            case "first":
            case "january": return JANUARY;
            case "2":
            case "feb":
            case "february": return FEBRUARY;
            case "3":
            case "mar":
            case "march": return MARCH;
            case "4":
            case "apr":
            case "april": return APRIL;
            case "5":
            case "may": return MAY;
            case "6":
            case "jun":
            case "june": return JUNE;
            case "7":
            case "jul":
            case "july": return JULY;
            case "8":
            case "aug":
            case "august": return AUGUST;
            case "9":
            case "sep":
            case "september": return SEPTEMBER;
            case "10":
            case "oct":
            case "october": return OCTOBER;
            case "11":
            case "nov":
            case "november": return NOVEMBER;
            case "12":
            case "dec":
            case "last":
            case "december": return DECEMBER;
            default:
                throw new ElasticsearchParseException("unknown month [{}]", day);
        }
    }


    @Override
    public String toString() {
        return cronKey;
    }
}
