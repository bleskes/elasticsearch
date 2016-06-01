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

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.watcher.condition.ConditionFactory;
import org.elasticsearch.xpack.support.clock.Clock;

import java.io.IOException;

public class ArrayCompareConditionFactory extends ConditionFactory<ArrayCompareCondition, ArrayCompareCondition.Result,
        ExecutableArrayCompareCondition> {

    private final Clock clock;

    @Inject
    public ArrayCompareConditionFactory(Settings settings, Clock clock) {
        super(Loggers.getLogger(ExecutableArrayCompareCondition.class, settings));
        this.clock = clock;
    }

    @Override
    public String type() {
        return ArrayCompareCondition.TYPE;
    }

    @Override
    public ArrayCompareCondition parseCondition(String watchId, XContentParser parser) throws IOException {
        return ArrayCompareCondition.parse(watchId, parser);
    }

    @Override
    public ExecutableArrayCompareCondition createExecutable(ArrayCompareCondition condition) {
        return new ExecutableArrayCompareCondition(condition, conditionLogger, clock);
    }
}
