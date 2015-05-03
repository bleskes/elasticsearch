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

import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.watcher.WatcherException;
import org.elasticsearch.watcher.actions.ActionRegistry;
import org.elasticsearch.watcher.actions.ActionWrapper;
import org.elasticsearch.watcher.actions.ExecutableActions;
import org.elasticsearch.watcher.condition.Condition;
import org.elasticsearch.watcher.condition.ConditionRegistry;
import org.elasticsearch.watcher.execution.WatchExecutionContext;
import org.elasticsearch.watcher.execution.Wid;
import org.elasticsearch.watcher.input.Input;
import org.elasticsearch.watcher.input.InputRegistry;
import org.elasticsearch.watcher.support.WatcherDateUtils;
import org.elasticsearch.watcher.throttle.Throttler;
import org.elasticsearch.watcher.transform.Transform;
import org.elasticsearch.watcher.transform.TransformRegistry;

import java.io.IOException;

import static org.elasticsearch.common.joda.time.DateTimeZone.UTC;

/**
*
*/
public class WatchExecutionResult implements ToXContent {

    private final DateTime executionTime;
    private final Input.Result inputResult;
    private final Condition.Result conditionResult;
    private final Throttler.Result throttleResult;
    private final @Nullable Transform.Result transformResult;
    private final ExecutableActions.Results actionsResults;

    public WatchExecutionResult(WatchExecutionContext context) {
        this(context.executionTime(), context.inputResult(), context.conditionResult(), context.throttleResult(), context.transformResult(), context.actionsResults());
    }

    WatchExecutionResult(DateTime executionTime, Input.Result inputResult, Condition.Result conditionResult, Throttler.Result throttleResult, @Nullable Transform.Result transformResult, ExecutableActions.Results actionsResults) {
        this.executionTime = executionTime;
        this.inputResult = inputResult;
        this.conditionResult = conditionResult;
        this.throttleResult = throttleResult;
        this.transformResult = transformResult;
        this.actionsResults = actionsResults;
    }

    public DateTime executionTime() {
        return executionTime;
    }

    public Input.Result inputResult() {
        return inputResult;
    }

    public Condition.Result conditionResult() {
        return conditionResult;
    }

    public Throttler.Result throttleResult() {
        return throttleResult;
    }

    public Transform.Result transformResult() {
        return transformResult;
    }

    public ExecutableActions.Results actionsResults() {
        return actionsResults;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        WatcherDateUtils.writeDate(Parser.EXECUTION_TIME_FIELD.getPreferredName(), builder, executionTime);
        if (inputResult != null) {
            builder.startObject(Parser.INPUT_RESULT_FIELD.getPreferredName())
                    .field(inputResult.type(), inputResult, params)
                    .endObject();
        }
        if (conditionResult != null) {
            builder.startObject(Parser.CONDITION_RESULT_FIELD.getPreferredName())
                    .field(conditionResult.type(), conditionResult, params)
                    .endObject();
        }
        if (throttleResult != null && throttleResult.throttle()) {
            builder.field(Parser.THROTTLED.getPreferredName(), throttleResult.throttle());
            if (throttleResult.reason() != null) {
                builder.field(Parser.THROTTLE_REASON.getPreferredName(), throttleResult.reason());
            }
        }
        if (transformResult != null) {
            builder.startObject(Transform.Field.TRANSFORM_RESULT.getPreferredName())
                    .field(transformResult.type(), transformResult, params)
                    .endObject();
        }
        builder.startObject(Parser.ACTIONS_RESULTS.getPreferredName());
        for (ActionWrapper.Result actionResult : actionsResults) {
            builder.field(actionResult.id(), actionResult, params);
        }
        builder.endObject();
        builder.endObject();
        return builder;
    }

    public static class Parser {

        public static final ParseField EXECUTION_TIME_FIELD = new ParseField("execution_time");
        public static final ParseField INPUT_RESULT_FIELD = new ParseField("input_result");
        public static final ParseField CONDITION_RESULT_FIELD = new ParseField("condition_result");
        public static final ParseField ACTIONS_RESULTS = new ParseField("actions_results");
        public static final ParseField THROTTLED = new ParseField("throttled");
        public static final ParseField THROTTLE_REASON = new ParseField("throttle_reason");

        public static WatchExecutionResult parse(Wid wid, XContentParser parser, ConditionRegistry conditionRegistry, ActionRegistry actionRegistry,
                                           InputRegistry inputRegistry, TransformRegistry transformRegistry) throws IOException {
            DateTime executionTime = null;
            boolean throttled = false;
            String throttleReason = null;
            ExecutableActions.Results actionResults = null;
            Input.Result inputResult = null;
            Condition.Result conditionResult = null;
            Transform.Result transformResult = null;

            String currentFieldName = null;
            XContentParser.Token token;
            while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                if (token == XContentParser.Token.FIELD_NAME) {
                    currentFieldName = parser.currentName();
                } else if (EXECUTION_TIME_FIELD.match(currentFieldName)) {
                    try {
                        executionTime = WatcherDateUtils.parseDate(currentFieldName, parser, UTC);
                    } catch (WatcherDateUtils.ParseException pe) {
                        throw new WatcherException("unable to parse watch execution [{}]. failed to parse date field [{}]", pe, wid, currentFieldName);
                    }
                } else if (token.isValue()) {
                    if (THROTTLE_REASON.match(currentFieldName)) {
                        throttleReason = parser.text();
                    } else if (THROTTLED.match(currentFieldName)) {
                        throttled = parser.booleanValue();
                    } else {
                        throw new WatcherException("unable to parse watch execution [{}]. unexpected field [{}]", wid, currentFieldName);
                    }
                } else if (token == XContentParser.Token.START_OBJECT) {
                    if (INPUT_RESULT_FIELD.match(currentFieldName)) {
                        inputResult = inputRegistry.parseResult(wid.watchId(), parser);
                    } else if (CONDITION_RESULT_FIELD.match(currentFieldName)) {
                        conditionResult = conditionRegistry.parseResult(wid.watchId(), parser);
                    } else if (Transform.Field.TRANSFORM_RESULT.match(currentFieldName)) {
                        transformResult = transformRegistry.parseResult(wid.watchId(), parser);
                    } else if (ACTIONS_RESULTS.match(currentFieldName)) {
                        actionResults = actionRegistry.parseResults(wid, parser);
                    } else {
                        throw new WatcherException("unable to parse watch execution. unexpected field [" + currentFieldName + "]");
                    }
                } else {
                    throw new WatcherException("unable to parse watch execution. unexpected token [" + token + "]");
                }
            }

            if (executionTime == null) {
                throw new WatcherException("unable to parse watch execution [{}]. missing required date field [{}]", wid, EXECUTION_TIME_FIELD.getPreferredName());
            }
            Throttler.Result throttleResult = throttled ? Throttler.Result.throttle(throttleReason) : Throttler.Result.NO;
            return new WatchExecutionResult(executionTime, inputResult, conditionResult, throttleResult, transformResult, actionResults);

        }
    }
}
