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
import org.elasticsearch.xpack.support.clock.Clock;
import org.elasticsearch.xpack.watcher.support.xcontent.ObjectPath;

import java.util.Map;


/**
 *
 */
public class ExecutableCompareCondition extends AbstractExecutableCompareCondition<CompareCondition, CompareCondition.Result> {
    public ExecutableCompareCondition(CompareCondition condition, ESLogger logger, Clock clock) {
        super(condition, logger, clock);
    }

    @Override
    protected CompareCondition.Result doExecute(Map<String, Object> model, Map<String, Object> resolvedValues) throws Exception {
        Object configuredValue = resolveConfiguredValue(resolvedValues, model, condition.getValue());

        Object resolvedValue = ObjectPath.eval(condition.getPath(), model);
        resolvedValues.put(condition.getPath(), resolvedValue);

        return new CompareCondition.Result(resolvedValues, condition.getOp().eval(resolvedValue, configuredValue));
    }

    @Override
    protected CompareCondition.Result doFailure(Map<String, Object> resolvedValues, Exception e) {
        return new CompareCondition.Result(resolvedValues, e);
    }
}
