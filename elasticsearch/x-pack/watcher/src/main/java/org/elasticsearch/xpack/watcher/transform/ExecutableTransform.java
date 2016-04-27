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

package org.elasticsearch.xpack.watcher.transform;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.xpack.watcher.execution.WatchExecutionContext;
import org.elasticsearch.xpack.watcher.watch.Payload;

import java.io.IOException;

/**
 *
 */
public abstract class ExecutableTransform<T extends Transform, R extends Transform.Result> implements ToXContent {

    protected final T transform;
    protected final ESLogger logger;

    public ExecutableTransform(T transform, ESLogger logger) {
        this.transform = transform;
        this.logger = logger;
    }

    public final String type() {
        return transform.type();
    }

    public T transform() {
        return transform;
    }

    public abstract R execute(WatchExecutionContext ctx, Payload payload);

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return transform.toXContent(builder, params);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExecutableTransform<?, ?> that = (ExecutableTransform<?, ?>) o;

        return transform.equals(that.transform);
    }

    @Override
    public int hashCode() {
        return transform.hashCode();
    }

}
