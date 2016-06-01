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

package org.elasticsearch.xpack.trigger.manual;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.trigger.TriggerEvent;
import org.elasticsearch.xpack.trigger.TriggerService;

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

    @Override
    public void recordDataXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(ManualTriggerEngine.TYPE);
        triggerEvent.recordDataXContent(builder, params);
        builder.endObject();
    }

    public static ManualTriggerEvent parse(TriggerService triggerService, String id, String context, XContentParser parser) throws
            IOException {
        TriggerEvent parsedTriggerEvent = triggerService.parseTriggerEvent(id, context, parser);
        return new ManualTriggerEvent(context, parsedTriggerEvent);
    }

}
