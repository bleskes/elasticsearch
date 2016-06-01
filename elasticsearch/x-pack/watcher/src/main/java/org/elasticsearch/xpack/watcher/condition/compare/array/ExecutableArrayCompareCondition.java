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

package org.elasticsearch.xpack.watcher.condition.compare.array;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.xpack.watcher.condition.compare.AbstractExecutableCompareCondition;
import org.elasticsearch.xpack.support.clock.Clock;
import org.elasticsearch.xpack.watcher.support.xcontent.ObjectPath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ExecutableArrayCompareCondition extends AbstractExecutableCompareCondition<ArrayCompareCondition,
        ArrayCompareCondition.Result> {

    public ExecutableArrayCompareCondition(ArrayCompareCondition condition, ESLogger logger, Clock clock) {
        super(condition, logger, clock);
    }

    @SuppressWarnings("unchecked")
    public ArrayCompareCondition.Result doExecute(Map<String, Object> model, Map<String, Object> resolvedValues) throws Exception {
        Object configuredValue = resolveConfiguredValue(resolvedValues, model, condition.getValue());

        Object object = ObjectPath.eval(condition.getArrayPath(), model);
        if (object != null && !(object instanceof List)) {
            throw new IllegalStateException("array path " + condition.getArrayPath() + " did not evaluate to array, was " + object);
        }

        List<Object> resolvedArray = object != null ? (List<Object>) object : Collections.emptyList();

        List<Object> resolvedValue = new ArrayList<>(resolvedArray.size());
        for (int i = 0; i < resolvedArray.size(); i++) {
            resolvedValue.add(ObjectPath.eval(condition.getPath(), resolvedArray.get(i)));
        }
        resolvedValues.put(condition.getArrayPath(), resolvedArray);

        return new ArrayCompareCondition.Result(resolvedValues, condition.getQuantifier().eval(resolvedValue, configuredValue,
                condition.getOp()));
    }

    @Override
    protected ArrayCompareCondition.Result doFailure(Map<String, Object> resolvedValues, Exception e) {
        return new ArrayCompareCondition.Result(resolvedValues, e);
    }
}
