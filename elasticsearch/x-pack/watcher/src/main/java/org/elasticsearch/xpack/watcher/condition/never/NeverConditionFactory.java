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

package org.elasticsearch.xpack.watcher.condition.never;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.watcher.condition.ConditionFactory;

import java.io.IOException;

/**
 *
 */
public class NeverConditionFactory extends ConditionFactory<NeverCondition, NeverCondition.Result, ExecutableNeverCondition> {

    private final ExecutableNeverCondition condition;

    @Inject
    public NeverConditionFactory(Settings settings) {
        super(Loggers.getLogger(ExecutableNeverCondition.class, settings));
        condition = new ExecutableNeverCondition(conditionLogger);
    }

    @Override
    public String type() {
        return NeverCondition.TYPE;
    }

    @Override
    public NeverCondition parseCondition(String watchId, XContentParser parser) throws IOException {
        return NeverCondition.parse(watchId, parser);
    }

    @Override
    public ExecutableNeverCondition createExecutable(NeverCondition condition) {
        return this.condition;
    }
}
