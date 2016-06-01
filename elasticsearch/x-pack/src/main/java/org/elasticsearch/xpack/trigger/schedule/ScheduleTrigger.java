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

package org.elasticsearch.xpack.trigger.schedule;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.xpack.trigger.Trigger;

import java.io.IOException;

/**
 *
 */
public class ScheduleTrigger implements Trigger {

    public static final String TYPE = "schedule";

    private final Schedule schedule;

    public ScheduleTrigger(Schedule schedule) {
        this.schedule = schedule;
    }

    @Override
    public String type() {
        return TYPE;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ScheduleTrigger trigger = (ScheduleTrigger) o;

        if (!schedule.equals(trigger.schedule)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return schedule.hashCode();
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.startObject().field(schedule.type(), schedule, params).endObject();
    }

    public static Builder builder(Schedule schedule) {
        return new Builder(schedule);
    }

    public static class Builder implements Trigger.Builder<ScheduleTrigger> {

        private final Schedule schedule;

        private Builder(Schedule schedule) {
            this.schedule = schedule;
        }

        @Override
        public ScheduleTrigger build() {
            return new ScheduleTrigger(schedule);
        }
    }
}
