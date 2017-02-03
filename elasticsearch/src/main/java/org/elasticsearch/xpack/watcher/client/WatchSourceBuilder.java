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

package org.elasticsearch.xpack.watcher.client;

import org.elasticsearch.action.support.ToXContentToBytes;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.xpack.watcher.actions.Action;
import org.elasticsearch.xpack.watcher.actions.throttler.Throttler;
import org.elasticsearch.xpack.watcher.condition.AlwaysCondition;
import org.elasticsearch.xpack.watcher.condition.Condition;
import org.elasticsearch.xpack.watcher.input.Input;
import org.elasticsearch.xpack.watcher.input.none.NoneInput;
import org.elasticsearch.xpack.watcher.support.Exceptions;
import org.elasticsearch.xpack.watcher.support.xcontent.XContentSource;
import org.elasticsearch.xpack.watcher.transform.Transform;
import org.elasticsearch.xpack.watcher.trigger.Trigger;
import org.elasticsearch.xpack.watcher.watch.Watch;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class WatchSourceBuilder extends ToXContentToBytes implements ToXContent {
    private Trigger trigger;
    private Input input = NoneInput.INSTANCE;
    private Condition condition = AlwaysCondition.INSTANCE;
    private Transform transform = null;
    private Map<String, TransformedAction> actions = new HashMap<>();
    private TimeValue defaultThrottlePeriod = null;
    private Map<String, Object> metadata;

    public WatchSourceBuilder trigger(Trigger.Builder trigger) {
        return trigger(trigger.build());
    }

    public WatchSourceBuilder trigger(Trigger trigger) {
        this.trigger = trigger;
        return this;
    }

    public WatchSourceBuilder input(Input.Builder input) {
        return input(input.build());
    }

    public WatchSourceBuilder input(Input input) {
        this.input = input;
        return this;
    }

    public WatchSourceBuilder condition(Condition condition) {
        this.condition = condition;
        return this;
    }

    public WatchSourceBuilder transform(Transform transform) {
        this.transform = transform;
        return this;
    }

    public WatchSourceBuilder transform(Transform.Builder transform) {
        return transform(transform.build());
    }

    public WatchSourceBuilder defaultThrottlePeriod(TimeValue throttlePeriod) {
        this.defaultThrottlePeriod = throttlePeriod;
        return this;
    }

    public WatchSourceBuilder addAction(String id, Action.Builder action) {
        return addAction(id, null, null, action.build());
    }

    public WatchSourceBuilder addAction(String id, TimeValue throttlePeriod, Action.Builder action) {
        return addAction(id, throttlePeriod, null, action.build());
    }

    public WatchSourceBuilder addAction(String id, Transform.Builder transform, Action.Builder action) {
        return addAction(id, null, transform.build(), action.build());
    }

    public WatchSourceBuilder addAction(String id, Condition condition, Action.Builder action) {
        return addAction(id, null, condition, null, action.build());
    }

    public WatchSourceBuilder addAction(String id, TimeValue throttlePeriod, Transform.Builder transform, Action.Builder action) {
        return addAction(id, throttlePeriod, transform.build(), action.build());
    }

    public WatchSourceBuilder addAction(String id, TimeValue throttlePeriod, Transform transform, Action action) {
        actions.put(id, new TransformedAction(id, action, throttlePeriod, null, transform));
        return this;
    }

    public WatchSourceBuilder addAction(String id, TimeValue throttlePeriod, Condition condition, Transform.Builder transform,
                                        Action.Builder action) {
        return addAction(id, throttlePeriod, condition, transform.build(), action.build());
    }

    public WatchSourceBuilder addAction(String id, TimeValue throttlePeriod, Condition condition, Transform transform, Action action) {
        actions.put(id, new TransformedAction(id, action, throttlePeriod, condition, transform));
        return this;
    }

    public WatchSourceBuilder metadata(Map<String, Object> metadata) {
        this.metadata = metadata;
        return this;
    }

    public XContentSource build() throws IOException {
        return new XContentSource(toXContent(jsonBuilder(), ToXContent.EMPTY_PARAMS));
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();

        if (trigger == null) {
            throw Exceptions.illegalState("failed to build watch source. no trigger defined");
        }
        builder.startObject(Watch.Field.TRIGGER.getPreferredName())
                .field(trigger.type(), trigger, params)
                .endObject();

        builder.startObject(Watch.Field.INPUT.getPreferredName())
                .field(input.type(), input, params)
                .endObject();

        builder.startObject(Watch.Field.CONDITION.getPreferredName())
                .field(condition.type(), condition, params)
                .endObject();

        if (transform != null) {
            builder.startObject(Watch.Field.TRANSFORM.getPreferredName())
                    .field(transform.type(), transform, params)
                    .endObject();
        }

        if (defaultThrottlePeriod != null) {
            builder.timeValueField(Watch.Field.THROTTLE_PERIOD.getPreferredName(),
                    Watch.Field.THROTTLE_PERIOD_HUMAN.getPreferredName(), defaultThrottlePeriod);
        }

        builder.startObject(Watch.Field.ACTIONS.getPreferredName());
        for (Map.Entry<String, TransformedAction> entry : actions.entrySet()) {
            builder.field(entry.getKey(), entry.getValue(), params);
        }
        builder.endObject();

        if (metadata != null) {
            builder.field(Watch.Field.METADATA.getPreferredName(), metadata);
        }

        return builder.endObject();
    }

    static class TransformedAction implements ToXContentObject {

        private final String id;
        private final Action action;
        @Nullable private final TimeValue throttlePeriod;
        @Nullable private final Condition condition;
        @Nullable private final Transform transform;

        TransformedAction(String id, Action action, @Nullable TimeValue throttlePeriod,
                                 @Nullable Condition condition, @Nullable Transform transform) {
            this.id = id;
            this.throttlePeriod = throttlePeriod;
            this.condition = condition;
            this.transform = transform;
            this.action = action;
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            if (throttlePeriod != null) {
                builder.timeValueField(Throttler.Field.THROTTLE_PERIOD.getPreferredName(),
                        Throttler.Field.THROTTLE_PERIOD_HUMAN.getPreferredName(), throttlePeriod);
            }
            if (condition != null) {
                builder.startObject(Watch.Field.CONDITION.getPreferredName())
                        .field(condition.type(), condition, params)
                        .endObject();
            }
            if (transform != null) {
                builder.startObject(Transform.Field.TRANSFORM.getPreferredName())
                        .field(transform.type(), transform, params)
                        .endObject();
            }
            builder.field(action.type(), action, params);
            return builder.endObject();
        }
    }
}
