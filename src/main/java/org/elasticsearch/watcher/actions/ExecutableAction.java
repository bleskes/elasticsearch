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

package org.elasticsearch.watcher.actions;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.support.LoggerMessageFormat;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.watcher.execution.WatchExecutionContext;
import org.elasticsearch.watcher.watch.Payload;

import java.io.IOException;

/**
 */
public abstract class ExecutableAction<A extends Action, R extends Action.Result> implements ToXContent {

    protected final A action;
    protected final ESLogger logger;

    protected ExecutableAction(A action, ESLogger logger) {
        this.action = action;
        this.logger = logger;
    }

    /**
     * @return the type of this action
     */
    public final String type() {
        return action.type();
    }

    public A action() {
        return action;
    }

    public R execute(String actionId, WatchExecutionContext context, Payload payload) throws IOException {
        try {
            return doExecute(actionId, context, payload);
        } catch (Exception e){
            logger.error("failed to execute [{}] action [{}/{}]", e, type(), context.id().value(), actionId);
            return failure(LoggerMessageFormat.format("failed to execute [{}] action [{}/{}]. error: {}", (Object) type(), context.id().value(), actionId, e.getMessage()));
        }
    }

    /**
     * Executes the action. The implementation need not to worry about handling exception/errors as they're handled
     * here by default. Of course, if the implementation wants to do that, it can... nothing stops you.
     */
    protected abstract R doExecute(String actionId, WatchExecutionContext context, Payload payload) throws Exception;

    /**
     * Returns an appropriate failure result that contains the given failure reason.
     */
    protected abstract R failure(String reason);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExecutableAction that = (ExecutableAction) o;

        return action.equals(that.action);
    }

    @Override
    public int hashCode() {
        return action.hashCode();
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return action.toXContent(builder, params);
    }

}
