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

package org.elasticsearch.watcher.watch;

import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.watcher.actions.Actions;
import org.elasticsearch.watcher.actions.ActionWrapper;
import org.elasticsearch.watcher.condition.Condition;
import org.elasticsearch.watcher.input.Input;
import org.elasticsearch.watcher.throttle.Throttler;
import org.elasticsearch.watcher.transform.Transform;
import org.elasticsearch.watcher.trigger.TriggerEvent;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class WatchExecutionContext {

    private final String id;
    private final Watch watch;
    private final DateTime executionTime;
    private final TriggerEvent triggerEvent;

    private Input.Result inputResult;
    private Condition.Result conditionResult;
    private Throttler.Result throttleResult;
    private Transform.Result transformResult;
    private Map<String, ActionWrapper.Result> actionsResults = new HashMap<>();

    private Payload payload;

    public WatchExecutionContext(String id, Watch watch, DateTime executionTime, TriggerEvent triggerEvent) {
        this.id = id;
        this.watch = watch;
        this.executionTime = executionTime;
        this.triggerEvent = triggerEvent;
    }

    public String id() {
        return id;
    }

    public Watch watch() {
        return watch;
    }

    public DateTime executionTime() {
        return executionTime;
    }

    public TriggerEvent triggerEvent() {
        return triggerEvent;
    }

    public Payload payload() {
        return payload;
    }

    public void onInputResult(Input.Result inputResult) {
        this.inputResult = inputResult;
        this.payload = inputResult.payload();
    }

    public Input.Result inputResult() {
        return inputResult;
    }

    public void onConditionResult(Condition.Result conditionResult) {
        watch.status().onCheck(conditionResult.met(), executionTime);
        this.conditionResult = conditionResult;
    }

    public Condition.Result conditionResult() {
        return conditionResult;
    }

    public void onThrottleResult(Throttler.Result throttleResult) {
        this.throttleResult = throttleResult;
        if (throttleResult.throttle()) {
            watch.status().onThrottle(executionTime, throttleResult.reason());
        } else {
            watch.status().onExecution(executionTime);
        }
    }

    public Throttler.Result throttleResult() {
        return throttleResult;
    }

    public void onTransformResult(Transform.Result transformResult) {
        this.transformResult = transformResult;
        this.payload = transformResult.payload();
    }

    public Transform.Result transformResult() {
        return transformResult;
    }

    public void onActionResult(ActionWrapper.Result result) {
        actionsResults.put(result.id(), result);
    }

    public Actions.Results actionsResults() {
        return new Actions.Results(actionsResults);
    }

    public WatchExecution finish() {
        return new WatchExecution(this);
    }

}
