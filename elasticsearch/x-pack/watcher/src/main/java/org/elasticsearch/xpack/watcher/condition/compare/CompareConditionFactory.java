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

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.watcher.condition.ConditionFactory;
import org.elasticsearch.xpack.support.clock.Clock;

import java.io.IOException;

/**
 *
 */
public class CompareConditionFactory extends ConditionFactory<CompareCondition, CompareCondition.Result, ExecutableCompareCondition> {

    private final Clock clock;

    @Inject
    public CompareConditionFactory(Settings settings, Clock clock) {
        super(Loggers.getLogger(ExecutableCompareCondition.class, settings));
        this.clock = clock;
    }

    @Override
    public String type() {
        return CompareCondition.TYPE;
    }

    @Override
    public CompareCondition parseCondition(String watchId, XContentParser parser) throws IOException {
        return CompareCondition.parse(watchId, parser);
    }

    @Override
    public ExecutableCompareCondition createExecutable(CompareCondition condition) {
        return new ExecutableCompareCondition(condition, conditionLogger, clock);
    }
}
