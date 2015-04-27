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

import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.watcher.actions.Action;
import org.elasticsearch.watcher.actions.ActionWrapper;
import org.elasticsearch.watcher.condition.Condition;
import org.elasticsearch.watcher.input.Input;
import org.elasticsearch.watcher.trigger.manual.ManualTriggerEvent;
import org.elasticsearch.watcher.watch.Watch;

import java.util.Map;

import static org.elasticsearch.common.joda.time.DateTimeZone.UTC;

/**
 */
public class ManualExecutionContext extends WatchExecutionContext {

    private final Map<String, ActionExecutionMode> actionModes;
    private final boolean recordExecution;

    ManualExecutionContext(Watch watch, DateTime executionTime, ManualTriggerEvent triggerEvent,
                           TimeValue defaultThrottlePeriod, Input.Result inputResult, Condition.Result conditionResult,
                           Map<String, ActionExecutionMode> actionModes, boolean recordExecution) {

        super(watch, executionTime, triggerEvent, defaultThrottlePeriod);

        this.actionModes = actionModes;
        this.recordExecution = recordExecution;

        if (inputResult != null) {
            onInputResult(inputResult);
        }
        if (conditionResult != null) {
            onConditionResult(conditionResult);
        }
        ActionExecutionMode allMode = actionModes.get(Builder.ALL);
        if (allMode == null || allMode == ActionExecutionMode.SKIP) {
            boolean throttleAll = allMode == ActionExecutionMode.SKIP;
            for (ActionWrapper action : watch.actions()) {
                if (throttleAll) {
                    onActionResult(new ActionWrapper.Result(action.id(), new Action.Result.Throttled(action.action().type(), "manually skipped")));
                } else {
                    ActionExecutionMode mode = actionModes.get(action.id());
                    if (mode == ActionExecutionMode.SKIP) {
                        onActionResult(new ActionWrapper.Result(action.id(), new Action.Result.Throttled(action.action().type(), "manually skipped")));
                    }
                }
            }
        }
    }

    @Override
    public final boolean simulateAction(String actionId) {
        ActionExecutionMode mode = actionModes.get(Builder.ALL);
        if (mode == ActionExecutionMode.SIMULATE || mode == ActionExecutionMode.FORCE_SIMULATE) {
            return true;
        }
        mode = actionModes.get(actionId);
        return mode == ActionExecutionMode.SIMULATE || mode == ActionExecutionMode.FORCE_SIMULATE;
    }

    @Override
    public boolean skipThrottling(String actionId) {
        ActionExecutionMode mode = actionModes.get(Builder.ALL);
        if (mode == ActionExecutionMode.FORCE_EXECUTE || mode == ActionExecutionMode.FORCE_SIMULATE) {
            return true;
        }
        mode = actionModes.get(actionId);
        return mode == ActionExecutionMode.FORCE_EXECUTE || mode == ActionExecutionMode.FORCE_SIMULATE;
    }

    @Override
    public final boolean recordExecution() {
        return recordExecution;
    }

    public static Builder builder(Watch watch, ManualTriggerEvent event, TimeValue defaultThrottlePeriod) {
        return new Builder(watch, event, defaultThrottlePeriod);
    }

    public static class Builder {

        static final String ALL = "_all";

        private final Watch watch;
        private final ManualTriggerEvent triggerEvent;
        private final TimeValue defaultThrottlePeriod;
        protected DateTime executionTime;
        private boolean recordExecution = false;
        private ImmutableMap.Builder<String, ActionExecutionMode> actionModes = ImmutableMap.builder();
        private Input.Result inputResult;
        private Condition.Result conditionResult;

        private Builder(Watch watch, ManualTriggerEvent triggerEvent, TimeValue defaultThrottlePeriod) {
            this.watch = watch;
            assert triggerEvent != null;
            this.triggerEvent = triggerEvent;
            this.defaultThrottlePeriod = defaultThrottlePeriod;
        }

        public Builder executionTime(DateTime executionTime) {
            this.executionTime = executionTime;
            return this;
        }

        public Builder recordExecution(boolean recordExecution) {
            this.recordExecution = recordExecution;
            return this;
        }

        public Builder allActionsMode(ActionExecutionMode mode) {
            return actionMode(ALL, mode);
        }

        public Builder actionMode(String id, ActionExecutionMode mode) {
            if (ALL.equals(id)) {
                actionModes = ImmutableMap.builder();
            }
            actionModes.put(id, mode);
            return this;
        }

        public Builder withInput(Input.Result inputResult) {
            this.inputResult = inputResult;
            return this;
        }

        public Builder withCondition(Condition.Result conditionResult) {
            this.conditionResult = conditionResult;
            return this;
        }

        public ManualExecutionContext build() {
            if (executionTime == null) {
                executionTime = DateTime.now(UTC);
            }
            return new ManualExecutionContext(watch, executionTime, triggerEvent, defaultThrottlePeriod, inputResult, conditionResult, actionModes.build(), recordExecution);
        }
    }
}
