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

package org.elasticsearch.xpack.watcher.input;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.xpack.watcher.execution.WatchExecutionContext;
import org.elasticsearch.xpack.watcher.watch.Payload;

import java.io.IOException;

/**
 *
 */
public abstract class ExecutableInput<I extends Input, R extends Input.Result> implements ToXContent {

    protected final I input;
    protected final ESLogger logger;

    protected ExecutableInput(I input, ESLogger logger) {
        this.input = input;
        this.logger = logger;
    }

    /**
     * @return the type of this input
     */
    public final String type() {
        return input.type();
    }

    public I input() {
        return input;
    }

    /**
     * Executes this input
     */
    public abstract R execute(WatchExecutionContext ctx, @Nullable Payload payload);

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return input.toXContent(builder, params);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExecutableInput<?, ?> that = (ExecutableInput<?, ?>) o;

        return input.equals(that.input);
    }

    @Override
    public int hashCode() {
        return input.hashCode();
    }
}
