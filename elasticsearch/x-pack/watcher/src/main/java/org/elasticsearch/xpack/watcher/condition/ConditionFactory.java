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

package org.elasticsearch.xpack.watcher.condition;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;

/**
 * Parses xcontent to a concrete condition of the same type.
 */
public abstract class ConditionFactory<C extends Condition, R extends Condition.Result, E extends ExecutableCondition<C, R>> {

    protected final ESLogger conditionLogger;

    public ConditionFactory(ESLogger conditionLogger) {
        this.conditionLogger = conditionLogger;
    }

    /**
     * @return  The type of the condition
     */
    public abstract String type();

    /**
     * Parses the given xcontent and creates a concrete condition
     */
    public abstract C parseCondition(String watchId, XContentParser parser) throws IOException;

    /**
     * Creates an {@link ExecutableCondition executable condition} for the given condition.
     */
    public abstract E createExecutable(C condition);
}
