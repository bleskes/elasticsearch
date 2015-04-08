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

package org.elasticsearch.watcher.trigger.schedule;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.watcher.WatcherSettingsException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class CronSchedule extends CronnableSchedule {

    public static final String TYPE = "cron";

    public CronSchedule(String... crons) {
        super(validate(crons));
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return crons.length == 1 ? builder.value(crons[0]) : builder.value(crons);
    }

    static String[] validate(String... crons) {
        for (String cron :crons) {
            try {
                Cron.validate(cron);
            } catch (Cron.ParseException pe) {
                throw new ValidationException(cron, pe);
            }
        }
        return crons;
    }

    public static class Parser implements Schedule.Parser<CronSchedule> {

        @Override
        public String type() {
            return TYPE;
        }

        @Override
        public CronSchedule parse(XContentParser parser) throws IOException {
            try {

                XContentParser.Token token = parser.currentToken();
                if (token == XContentParser.Token.VALUE_STRING) {
                    return new CronSchedule(parser.text());
                } else if (token == XContentParser.Token.START_ARRAY) {
                    List<String> crons = new ArrayList<>();
                    while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
                        switch (token) {
                            case VALUE_STRING:
                                crons.add(parser.text());
                                break;
                            default:
                                throw new WatcherSettingsException("could not parse [cron] schedule. expected a string value in the cron array but found [" + token + "]");
                        }
                    }
                    if (crons.isEmpty()) {
                        throw new WatcherSettingsException("could not parse [cron] schedule. no cron expression found in cron array");
                    }
                    return new CronSchedule(crons.toArray(new String[crons.size()]));
                } else {
                    throw new WatcherSettingsException("could not parse [cron] schedule. expected either a cron string value or an array of cron string values, but found [" + token + "]");
                }

            } catch (ValidationException ve) {
                throw new WatcherSettingsException("could not parse [cron] schedule. invalid cron expression [" + ve.expression + "]", ve);
            }
        }
    }

    public static class ValidationException extends WatcherSettingsException {

        private String expression;

        public ValidationException(String expression, Cron.ParseException cause) {
            super("invalid cron expression [" + expression + "]. " + cause.getMessage());
            this.expression = expression;
        }
    }
}
