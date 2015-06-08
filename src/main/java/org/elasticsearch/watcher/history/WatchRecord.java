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

package org.elasticsearch.watcher.history;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.watcher.condition.Condition;
import org.elasticsearch.watcher.execution.ExecutionState;
import org.elasticsearch.watcher.execution.WatchExecutionContext;
import org.elasticsearch.watcher.execution.WatchExecutionResult;
import org.elasticsearch.watcher.execution.Wid;
import org.elasticsearch.watcher.input.ExecutableInput;
import org.elasticsearch.watcher.trigger.TriggerEvent;
import org.elasticsearch.watcher.watch.Watch;

import java.io.IOException;
import java.util.Map;

public class WatchRecord implements ToXContent {

    private final Wid id;
    private final TriggerEvent triggerEvent;
    private final ExecutionState state;

    private final @Nullable ExecutableInput input;
    private final @Nullable Condition condition;
    private final @Nullable Map<String,Object> metadata;

    private final @Nullable String message;
    private final @Nullable WatchExecutionResult executionResult;

    /**
     * Called when the execution was aborted before it started
     */
    public WatchRecord(Wid id, TriggerEvent triggerEvent, String message, ExecutionState state) {
        this.id = id;
        this.triggerEvent = triggerEvent;
        this.executionResult = null;
        this.state = state;
        this.message = message;
        this.condition = null;
        this.input = null;
        this.metadata = null;
    }

    /**
     * Called when the execution was aborted due to an error during execution (the given result should reflect
     * were exactly the execution failed)
     */
    public WatchRecord(WatchExecutionContext context, WatchExecutionResult executionResult, String message) {
        this.id = context.id();
        this.triggerEvent = context.triggerEvent();
        this.condition = context.watch().condition().condition();
        this.input = context.watch().input();
        this.metadata = context.watch().metadata();
        this.executionResult = executionResult;
        this.message = message;
        this.state = ExecutionState.FAILED;
    }

    /**
     * Called when the execution finished.
     */
    public WatchRecord(WatchExecutionContext context, WatchExecutionResult executionResult) {
        this.id = context.id();
        this.triggerEvent = context.triggerEvent();
        this.condition = context.watch().condition().condition();
        this.input = context.watch().input();
        this.executionResult = executionResult;
        this.metadata = context.watch().metadata();
        this.message = null;

        if (!this.executionResult.conditionResult().met()) {
            state = ExecutionState.EXECUTION_NOT_NEEDED;
        } else {
            if (this.executionResult.actionsResults().throttled()) {
                state = ExecutionState.THROTTLED;
            } else {
                state = ExecutionState.EXECUTED;
            }
        }
    }

    public Wid id() {
        return id;
    }

    public TriggerEvent triggerEvent() {
        return triggerEvent;
    }

    public String watchId() {
        return id.watchId();
    }

    public ExecutableInput input() { return input; }

    public Condition condition() {
        return condition;
    }

    public ExecutionState state() {
        return state;
    }

    public String message(){
        return this.message;
    }

    public Map<String, Object> metadata() {
        return metadata;
    }

    public WatchExecutionResult result() {
        return executionResult;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(Field.WATCH_ID.getPreferredName(), id.watchId());
        builder.field(Field.STATE.getPreferredName(), state.id());

        builder.field(Field.TRIGGER_EVENT.getPreferredName());
        triggerEvent.recordXContent(builder, params);

        if (input != null) {
            builder.startObject(Watch.Field.INPUT.getPreferredName())
                    .field(input.type(), input, params)
                    .endObject();
        }
        if (condition != null) {
            builder.startObject(Watch.Field.CONDITION.getPreferredName())
                    .field(condition.type(), condition, params)
                    .endObject();
        }

        if (message != null) {
            builder.field(Field.MESSAGE.getPreferredName(), message);
        }
        if (metadata != null) {
            builder.field(Field.METADATA.getPreferredName(), metadata);
        }

        if (executionResult != null) {
            builder.field(Field.EXECUTION_RESULT.getPreferredName(), executionResult, params);
        }

        builder.endObject();
        return builder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WatchRecord entry = (WatchRecord) o;
        if (!id.equals(entry.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id.toString();
    }

    public interface Field {
        ParseField WATCH_ID = new ParseField("watch_id");
        ParseField TRIGGER_EVENT = new ParseField("trigger_event");
        ParseField MESSAGE = new ParseField("message");
        ParseField STATE = new ParseField("state");
        ParseField METADATA = new ParseField("metadata");
        ParseField EXECUTION_RESULT = new ParseField("result");
    }
}
