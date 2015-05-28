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

package org.elasticsearch.watcher.execution;

import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.concurrent.ConcurrentCollections;
import org.elasticsearch.watcher.actions.ActionWrapper;
import org.elasticsearch.watcher.actions.ExecutableActions;
import org.elasticsearch.watcher.condition.Condition;
import org.elasticsearch.watcher.history.WatchRecord;
import org.elasticsearch.watcher.input.Input;
import org.elasticsearch.watcher.transform.ExecutableTransform;
import org.elasticsearch.watcher.transform.Transform;
import org.elasticsearch.watcher.trigger.TriggerEvent;
import org.elasticsearch.watcher.watch.Payload;
import org.elasticsearch.watcher.watch.Watch;

import java.io.IOException;
import java.util.concurrent.ConcurrentMap;

/**
 *
 */
public abstract class WatchExecutionContext {

    private final Wid id;
    private final Watch watch;
    private final DateTime executionTime;
    private final TriggerEvent triggerEvent;
    private final TimeValue defaultThrottlePeriod;

    private Input.Result inputResult;
    private Condition.Result conditionResult;
    private Transform.Result transformResult;
    private ConcurrentMap<String, ActionWrapper.Result> actionsResults = ConcurrentCollections.newConcurrentMap();

    private Payload payload;
    private Payload transformedPayload;

    private volatile ExecutionPhase executionPhase = ExecutionPhase.AWAITS_EXECUTION;

    private long actualExecutionStartMs;

    public WatchExecutionContext(Watch watch, DateTime executionTime, TriggerEvent triggerEvent, TimeValue defaultThrottlePeriod) {
        this.id = new Wid(watch.id(), watch.nonce(), executionTime);
        this.watch = watch;
        this.executionTime = executionTime;
        this.triggerEvent = triggerEvent;
        this.defaultThrottlePeriod = defaultThrottlePeriod;
    }

    /**
     * @return true if this action should be simulated
     */
    public abstract boolean simulateAction(String actionId);

    public abstract boolean skipThrottling(String actionId);

    /**
     * @return true if this execution should be recorded in the .watch_history index
     */
    public abstract boolean recordExecution();

    public Wid id() {
        return id;
    }

    public Watch watch() {
        return watch;
    }

    public DateTime executionTime() {
        return executionTime;
    }

    /**
     * @return The default throttle period in the system.
     */
    public TimeValue defaultThrottlePeriod() {
        return defaultThrottlePeriod;
    }

    public TriggerEvent triggerEvent() {
        return triggerEvent;
    }

    public Payload payload() {
        return payload;
    }

    /**
     * If a transform is associated with the watch itself, this method will return the transformed payload,
     * that is, the payload after the transform was applied to it. note, after calling this method, the payload
     * will be the same as the transformed payload. Also note, that the transform is only applied once. So calling
     * this method multiple times will always result in the same payload.
     */
    public Payload transformedPayload() throws IOException {
        if (transformedPayload != null) {
            return transformedPayload;
        }
        ExecutableTransform transform = watch.transform();
        if (transform == null) {
            transformedPayload = payload;
            return transformedPayload;
        }
        this.transformResult = watch.transform().execute(this, payload);
        this.payload = transformResult.payload();
        this.transformedPayload = this.payload;
        return transformedPayload;
    }

    public ExecutionPhase executionPhase() {
        return executionPhase;
    }

    public void beforeInput() {
        executionPhase = ExecutionPhase.INPUT;
    }

    public void onInputResult(Input.Result inputResult) {
        this.inputResult = inputResult;
        this.payload = inputResult.payload();
    }

    public Input.Result inputResult() {
        return inputResult;
    }

    public void beforeCondition() {
        executionPhase = ExecutionPhase.CONDITION;
    }

    public void onConditionResult(Condition.Result conditionResult) {
        this.conditionResult = conditionResult;
        if (recordExecution()) {
            watch.status().onCheck(conditionResult.met(), executionTime);
        }

    }

    public Condition.Result conditionResult() {
        return conditionResult;
    }

    public void beforeWatchTransform() {
        this.executionPhase = ExecutionPhase.WATCH_TRANSFORM;
    }

    public void onTransformResult(Transform.Result transformResult) {
        this.transformResult = transformResult;
        this.payload = transformResult.payload();
    }

    public Transform.Result transformResult() {
        return transformResult;
    }

    public void beforeAction() {
        executionPhase = ExecutionPhase.ACTIONS;
    }

    public void onActionResult(ActionWrapper.Result result) {
        actionsResults.put(result.id(), result);
        if (recordExecution()) {
            watch.status().onActionResult(result.id(), executionTime, result.action());
        }
    }

    public ExecutableActions.Results actionsResults() {
        return new ExecutableActions.Results(actionsResults);
    }

    public void start() {
        actualExecutionStartMs = System.currentTimeMillis();
    }

    public WatchRecord finish() {
        executionPhase = ExecutionPhase.FINISHED;
        long executionFinishMs = System.currentTimeMillis();
        WatchExecutionResult result = new WatchExecutionResult(this, executionFinishMs - actualExecutionStartMs);
        return new WatchRecord(this, result);
    }

    public WatchRecord abort(String message, ExecutionState state) {
        return new WatchRecord(id, triggerEvent, message, state);
    }

    public WatchExecutionSnapshot createSnapshot(Thread executionThread) {
        return new WatchExecutionSnapshot(this, executionThread.getStackTrace());
    }

}
