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

package org.elasticsearch.xpack.watcher.input.none;


import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.xpack.watcher.execution.WatchExecutionContext;
import org.elasticsearch.xpack.watcher.input.ExecutableInput;
import org.elasticsearch.xpack.watcher.watch.Payload;

/**
 *
 */
public class ExecutableNoneInput extends ExecutableInput<NoneInput, NoneInput.Result> {

    public ExecutableNoneInput(ESLogger logger) {
        super(NoneInput.INSTANCE, logger);
    }

    @Override
    public NoneInput.Result execute(WatchExecutionContext ctx, Payload payload) {
        return NoneInput.Result.INSTANCE;
    }

}
