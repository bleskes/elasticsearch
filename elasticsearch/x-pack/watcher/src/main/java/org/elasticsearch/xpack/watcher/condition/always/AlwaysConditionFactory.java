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

package org.elasticsearch.xpack.watcher.condition.always;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.watcher.condition.ConditionFactory;

import java.io.IOException;

/**
 *
 */
public class AlwaysConditionFactory extends ConditionFactory<AlwaysCondition, AlwaysCondition.Result, ExecutableAlwaysCondition> {

    private final ExecutableAlwaysCondition condition;

    @Inject
    public AlwaysConditionFactory(Settings settings) {
        super(Loggers.getLogger(ExecutableAlwaysCondition.class, settings));
        condition = new ExecutableAlwaysCondition(conditionLogger);
    }

    @Override
    public String type() {
        return AlwaysCondition.TYPE;
    }

    @Override
    public AlwaysCondition parseCondition(String watchId, XContentParser parser) throws IOException {
        return AlwaysCondition.parse(watchId, parser);
    }

    @Override
    public ExecutableAlwaysCondition createExecutable(AlwaysCondition condition) {
        return this.condition;
    }
}
