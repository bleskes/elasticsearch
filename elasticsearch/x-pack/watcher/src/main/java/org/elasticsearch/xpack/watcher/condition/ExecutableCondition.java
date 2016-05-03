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
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.xpack.watcher.execution.WatchExecutionContext;

import java.io.IOException;

/**
 *
 */
public abstract class ExecutableCondition<C extends Condition, R extends Condition.Result> implements ToXContent {

    protected final C condition;
    protected final ESLogger logger;

    protected ExecutableCondition(C condition, ESLogger logger) {
        this.condition = condition;
        this.logger = logger;
    }

    /**
     * @return the type of this condition
     */
    public final String type() {
        return condition.type();
    }

    public C condition() {
        return condition;
    }

    /**
     * Executes this condition
     */
    public abstract R execute(WatchExecutionContext ctx);

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return condition.toXContent(builder, params);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExecutableCondition<?, ?> that = (ExecutableCondition<?, ?>) o;

        return condition.equals(that.condition);
    }

    @Override
    public int hashCode() {
        return condition.hashCode();
    }
}
