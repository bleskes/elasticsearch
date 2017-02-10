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

package org.elasticsearch.xpack.watcher.trigger.schedule;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class ScheduleRegistry {
    private final Map<String, Schedule.Parser> parsers;

    @Inject
    public ScheduleRegistry(Map<String, Schedule.Parser> parsers) {
        this.parsers = parsers;
    }

    public Set<String> types() {
        return parsers.keySet();
    }

    public Schedule parse(String context, XContentParser parser) throws IOException {
        String type = null;
        XContentParser.Token token;
        Schedule schedule = null;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                type = parser.currentName();
            } else if (type != null) {
                schedule = parse(context, type, parser);
            } else {
                throw new ElasticsearchParseException("could not parse schedule. expected a schedule type field, but found [{}] instead",
                        token);
            }
        }
        if (schedule == null) {
            throw new ElasticsearchParseException("could not parse schedule. expected a schedule type field, but no fields were found");
        }
        return schedule;
    }

    public Schedule parse(String context, String type, XContentParser parser) throws IOException {
        Schedule.Parser scheduleParser = parsers.get(type);
        if (scheduleParser == null) {
            throw new ElasticsearchParseException("could not parse schedule for [{}]. unknown schedule type [{}]", context, type);
        }
        return scheduleParser.parse(parser);
    }
}
