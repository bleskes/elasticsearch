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

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.watcher.support.clock.Clock;
import org.elasticsearch.watcher.trigger.AbstractTriggerEngine;
import org.elasticsearch.watcher.trigger.TriggerService;

import java.io.IOException;

/**
 *
 */
public abstract class ScheduleTriggerEngine extends AbstractTriggerEngine<ScheduleTrigger, ScheduleTriggerEvent> {

    public static final String TYPE = ScheduleTrigger.TYPE;

    protected final ScheduleRegistry scheduleRegistry;
    protected final Clock clock;

    public ScheduleTriggerEngine(Settings settings, ScheduleRegistry scheduleRegistry, Clock clock) {
        super(settings);
        this.scheduleRegistry = scheduleRegistry;
        this.clock = clock;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public ScheduleTrigger parseTrigger(String context, XContentParser parser) throws IOException {
        Schedule schedule = scheduleRegistry.parse(context, parser);
        return new ScheduleTrigger(schedule);
    }

    @Override
    public ScheduleTriggerEvent parseTriggerEvent(TriggerService service, String watchId, String context, XContentParser parser) throws IOException {
        return ScheduleTriggerEvent.parse(parser, watchId, context, clock);
    }
}
