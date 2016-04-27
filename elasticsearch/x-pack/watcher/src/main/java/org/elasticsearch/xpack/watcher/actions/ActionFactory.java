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
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;

/**
 * Parses xcontent to a concrete action of the same type.
 */
public abstract class ActionFactory<A extends Action, E extends ExecutableAction<A>> {

    protected final ESLogger actionLogger;

    protected ActionFactory(ESLogger actionLogger) {
        this.actionLogger = actionLogger;
    }

    /**
     * @return  The type of the action
     */
    public abstract String type();

    public abstract A parseAction(String watchId, String actionId, XContentParser parser) throws IOException;

    public abstract E createExecutable(A action);

    /**
     * Parses the given xcontent and creates a concrete action
     */
    public E parseExecutable(String watchId, String actionId, XContentParser parser) throws IOException {
        A action = parseAction(watchId, actionId, parser);
        return createExecutable(action);
    }
}
