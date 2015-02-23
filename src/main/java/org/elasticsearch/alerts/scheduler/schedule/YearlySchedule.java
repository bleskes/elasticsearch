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

import org.elasticsearch.alerts.AlertsSettingsException;
import org.elasticsearch.alerts.scheduler.schedule.support.YearTimes;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class YearlySchedule extends CronnableSchedule {

    public static final String TYPE = "yearly";

    public static final YearTimes[] DEFAULT_TIMES = new YearTimes[] { new YearTimes() };

    private final YearTimes[] times;

    YearlySchedule() {
        this(DEFAULT_TIMES);
    }

    YearlySchedule(YearTimes... times) {
        super(crons(times));
        this.times = times;
    }

    @Override
    public String type() {
        return TYPE;
    }

    public YearTimes[] times() {
        return times;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        if (params.paramAsBoolean("normalize", false) && times.length == 1) {
            return builder.value(times[0]);
        }
        return builder.value(times);
    }

    public static Builder builder() {
        return new Builder();
    }

    static String[] crons(YearTimes[] times) {
        assert times.length > 0 : "at least one time must be defined";
        Set<String> crons = new HashSet<>(times.length);
        for (YearTimes time : times) {
            crons.addAll(time.crons());
        }
        return crons.toArray(new String[crons.size()]);
    }

    public static class Parser implements Schedule.Parser<YearlySchedule> {

        @Override
        public String type() {
            return TYPE;
        }

        @Override
        public YearlySchedule parse(XContentParser parser) throws IOException {
            if (parser.currentToken() == XContentParser.Token.START_OBJECT) {
                try {
                    return new YearlySchedule(YearTimes.parse(parser, parser.currentToken()));
                } catch (YearTimes.ParseException pe) {
                    throw new AlertsSettingsException("could not parse [yearly] schedule. invalid year times", pe);
                }
            }
            if (parser.currentToken() == XContentParser.Token.START_ARRAY) {
                List<YearTimes> times = new ArrayList<>();
                XContentParser.Token token;
                while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
                    try {
                        times.add(YearTimes.parse(parser, token));
                    } catch (YearTimes.ParseException pe) {
                        throw new AlertsSettingsException("could not parse [yearly] schedule. invalid year times", pe);
                    }
                }
                return times.isEmpty() ? new YearlySchedule() : new YearlySchedule(times.toArray(new YearTimes[times.size()]));
            }
            throw new AlertsSettingsException("could not parse [yearly] schedule. expected either an object or an array " +
                    "of objects representing year times, but found [" + parser.currentToken() + "] instead");
        }
    }

    public static class Builder {

        private final Set<YearTimes> times = new HashSet<>();

        private Builder() {
        }

        public Builder time(YearTimes time) {
            times.add(time);
            return this;
        }

        public Builder time(YearTimes.Builder builder) {
            return time(builder.build());
        }

        public YearlySchedule build() {
            return times.isEmpty() ? new YearlySchedule() : new YearlySchedule(times.toArray(new YearTimes[times.size()]));
        }
    }

}
