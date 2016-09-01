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

import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;

/**
 *
 */
public abstract class TransformFactory<T extends Transform, R extends Transform.Result, E extends ExecutableTransform<T, R>> {

    protected final Logger transformLogger;

    public TransformFactory(Logger transformLogger) {
        this.transformLogger = transformLogger;
    }

    /**
     * @return  The type of the transform
     */
    public abstract String type();

    /**
     * Parses the given xcontent and creates a concrete transform
     */
    public abstract T parseTransform(String watchId, XContentParser parser) throws IOException;

    /**
     * Creates an executable transform out of the given transform.
     */
    public abstract E createExecutable(T transform);

    public E parseExecutable(String watchId, XContentParser parser) throws IOException {
        T transform = parseTransform(watchId, parser);
        return createExecutable(transform);
    }
}
