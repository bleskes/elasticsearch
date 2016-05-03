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


package org.elasticsearch.xpack.watcher.input.simple;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.xpack.watcher.execution.WatchExecutionContext;
import org.elasticsearch.xpack.watcher.input.ExecutableInput;
import org.elasticsearch.xpack.watcher.watch.Payload;

/**
 * This class just defines a simple xcontent map as an input
 */
public class ExecutableSimpleInput extends ExecutableInput<SimpleInput, SimpleInput.Result> {

    public ExecutableSimpleInput(SimpleInput input, ESLogger logger) {
        super(input, logger);
    }

    @Override
    public SimpleInput.Result execute(WatchExecutionContext ctx, Payload payload) {
        return new SimpleInput.Result(input.getPayload());
    }
}
