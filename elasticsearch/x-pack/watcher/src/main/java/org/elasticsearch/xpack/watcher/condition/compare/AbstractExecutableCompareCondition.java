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

package org.elasticsearch.xpack.watcher.condition.compare;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.xpack.watcher.condition.Condition;
import org.elasticsearch.xpack.watcher.condition.ExecutableCondition;
import org.elasticsearch.xpack.watcher.execution.WatchExecutionContext;
import org.elasticsearch.xpack.watcher.support.Variables;
import org.elasticsearch.xpack.watcher.support.WatcherDateTimeUtils;
import org.elasticsearch.xpack.watcher.support.clock.Clock;
import org.elasticsearch.xpack.watcher.support.xcontent.ObjectPath;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractExecutableCompareCondition<C extends Condition, R extends Condition.Result>
        extends ExecutableCondition<C, R> {
    static final Pattern DATE_MATH_PATTERN = Pattern.compile("<\\{(.+)\\}>");
    static final Pattern PATH_PATTERN = Pattern.compile("\\{\\{(.+)\\}\\}");

    private final Clock clock;

    public AbstractExecutableCompareCondition(C condition, ESLogger logger, Clock clock) {
        super(condition, logger);
        this.clock = clock;
    }

    @Override
    public R execute(WatchExecutionContext ctx) {
        Map<String, Object> resolvedValues = new HashMap<>();
        try {
            Map<String, Object> model = Variables.createCtxModel(ctx, ctx.payload());
            return doExecute(model, resolvedValues);
        } catch (Exception e) {
            logger.error("failed to execute [{}] condition for [{}]", e, type(), ctx.id());
            if (resolvedValues.isEmpty()) {
                resolvedValues = null;
            }
            return doFailure(resolvedValues, e);
        }
    }

    protected Object resolveConfiguredValue(Map<String, Object> resolvedValues, Map<String, Object> model, Object configuredValue) {
        if (configuredValue instanceof String) {

            // checking if the given value is a date math expression
            Matcher matcher = DATE_MATH_PATTERN.matcher((String) configuredValue);
            if (matcher.matches()) {
                String dateMath = matcher.group(1);
                configuredValue = WatcherDateTimeUtils.parseDateMath(dateMath, DateTimeZone.UTC, clock);
                resolvedValues.put(dateMath, WatcherDateTimeUtils.formatDate((DateTime) configuredValue));
            } else {
                // checking if the given value is a path expression
                matcher = PATH_PATTERN.matcher((String) configuredValue);
                if (matcher.matches()) {
                    String configuredPath = matcher.group(1);
                    configuredValue = ObjectPath.eval(configuredPath, model);
                    resolvedValues.put(configuredPath, configuredValue);
                }
            }
        }
        return configuredValue;
    }

    protected abstract R doExecute(Map<String, Object> model, Map<String, Object> resolvedValues) throws Exception;

    protected abstract R doFailure(Map<String, Object> resolvedValues, Exception e);
}
