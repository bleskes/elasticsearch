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

package org.elasticsearch.xpack.watcher.actions;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.xpack.watcher.execution.WatchExecutionContext;
import org.elasticsearch.xpack.watcher.watch.Payload;

import java.io.IOException;

/**
 */
public abstract class ExecutableAction<A extends Action> implements ToXContent {

    protected final A action;
    protected final ESLogger logger;

    protected ExecutableAction(A action, ESLogger logger) {
        this.action = action;
        this.logger = logger;
    }

    /**
     * @return the type of this action
     */
    public String type() {
        return action.type();
    }

    public A action() {
        return action;
    }

    /**
     * yack... needed to expose that for testing purposes
     */
    public ESLogger logger() {
        return logger;
    }

    public abstract Action.Result execute(String actionId, WatchExecutionContext context, Payload payload) throws Exception;

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
