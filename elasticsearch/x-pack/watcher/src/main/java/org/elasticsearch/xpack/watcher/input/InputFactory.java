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

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;

/**
 * Parses xcontent to a concrete input of the same type.
 */
public abstract class InputFactory<I extends Input, R extends Input.Result, E extends ExecutableInput<I, R>> {

    protected final ESLogger inputLogger;

    public InputFactory(ESLogger inputLogger) {
        this.inputLogger = inputLogger;
    }

    /**
     * @return  The type of the input
     */
    public abstract String type();

    /**
     * Parses the given xcontent and creates a concrete input
     */
    public abstract I parseInput(String watchId, XContentParser parser) throws IOException;

    /**
     * Creates an executable input out of the given input.
     */
    public abstract E createExecutable(I input);

    public E parseExecutable(String watchId, XContentParser parser) throws IOException {
        I input = parseInput(watchId, parser);
        return createExecutable(input);
    }
}
