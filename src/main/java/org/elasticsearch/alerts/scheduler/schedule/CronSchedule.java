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

package org.elasticsearch.alerts.scheduler.schedule;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;

/**
 *
 */
public class CronSchedule implements Schedule {

    public static final String TYPE = "cron";

    private final String cron;

    public CronSchedule(String cron) {
        this.cron = cron;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public String cron() {
        return cron;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.value(cron);
    }

    public static class Parser implements Schedule.Parser<CronSchedule> {

        @Override
        public String type() {
            return TYPE;
        }

        @Override
        public CronSchedule parse(XContentParser parser) throws IOException {
            assert parser.currentToken() == XContentParser.Token.VALUE_STRING : "expecting a string value with cron expression";
            String cron = parser.text();
            parser.nextToken();
            return new CronSchedule(cron);
        }
    }
}
