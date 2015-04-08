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

package org.elasticsearch.watcher.trigger.manual;

import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.watcher.WatcherException;
import org.elasticsearch.watcher.support.WatcherDateUtils;
import org.elasticsearch.watcher.trigger.TriggerEvent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 */
public class ManualTriggerEvent extends TriggerEvent {

    private final static ParseField TRIGGER_DATA_FIELD = new ParseField("trigger_data");

    private final Map<String, Object> triggerData;

    public ManualTriggerEvent(DateTime triggeredTime, Map<String, Object> triggerData) {
        super(triggeredTime);
        data.putAll(triggerData);
        this.triggerData = triggerData;
    }

    @Override
    public String type() {
        return ManualTriggerEngine.TYPE;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.startObject()
                .field(TRIGGERED_TIME_FIELD.getPreferredName(), WatcherDateUtils.formatDate(triggeredTime))
                .field(TRIGGER_DATA_FIELD.getPreferredName(), triggerData)
                .endObject();
    }

    public static ManualTriggerEvent parse(String context, XContentParser parser) throws IOException {
        DateTime triggeredTime = null;
        Map<String, Object> triggerData = new HashMap<>();
        String currentFieldName = null;
        XContentParser.Token token;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else {
                if (token == XContentParser.Token.VALUE_STRING) {
                    if (TRIGGERED_TIME_FIELD.match(currentFieldName)) {
                        triggeredTime = WatcherDateUtils.parseDate(parser.text());
                    } else {
                        throw new ParseException("could not parse trigger event for [" + context + "]. unknown string value field [" + currentFieldName + "]");
                    }
                } if (token == XContentParser.Token.START_OBJECT) {
                    if (TRIGGER_DATA_FIELD.match(currentFieldName)) {
                        triggerData = parser.map();
                    } else {
                        throw new ParseException("could not parse trigger event for [" + context + "]. unknown object value field [" + currentFieldName + "]");
                    }
                } else {
                    throw new ParseException("could not parse trigger event for [" + context + "]. unexpected token [" + token + "]");
                }
            }
        }

        // should never be, it's fully controlled internally (not coming from the user)
        assert triggeredTime != null;
        return new ManualTriggerEvent(triggeredTime, triggerData);
    }

    public static class ParseException extends WatcherException {

        public ParseException(String msg) {
            super(msg);
        }

        public ParseException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }

}
