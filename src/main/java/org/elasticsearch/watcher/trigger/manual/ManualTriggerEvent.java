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

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.watcher.WatcherException;
import org.elasticsearch.watcher.trigger.TriggerEvent;
import org.elasticsearch.watcher.trigger.TriggerService;

import java.io.IOException;

/**
 */
public class ManualTriggerEvent extends TriggerEvent {

    private final TriggerEvent triggerEvent;


    public ManualTriggerEvent(String jobName, TriggerEvent triggerEvent) {
        super(jobName, triggerEvent.triggeredTime());
        this.triggerEvent = triggerEvent;
        data.putAll(triggerEvent.data());
    }

    @Override
    public String type() {
        return ManualTriggerEngine.TYPE;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(triggerEvent.type(), triggerEvent, params);
        return builder.endObject();
    }

    public static ManualTriggerEvent parse(TriggerService triggerService, String watchId, String context, XContentParser parser) throws IOException {
        TriggerEvent parsedTriggerEvent = triggerService.parseTriggerEvent(watchId, context, parser);
        return new ManualTriggerEvent(context, parsedTriggerEvent);
    }

    public static class ParseException extends WatcherException {

        public ParseException(String msg, Object... args) {
            super(msg, args);
        }

        public ParseException(String msg, Throwable cause, Object... args) {
            super(msg, cause, args);
        }
    }

}
