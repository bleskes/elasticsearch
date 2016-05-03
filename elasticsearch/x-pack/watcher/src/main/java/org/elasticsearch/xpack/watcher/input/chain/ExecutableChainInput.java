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

package org.elasticsearch.xpack.watcher.input.chain;

import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.xpack.watcher.execution.WatchExecutionContext;
import org.elasticsearch.xpack.watcher.input.ExecutableInput;
import org.elasticsearch.xpack.watcher.input.Input;
import org.elasticsearch.xpack.watcher.watch.Payload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExecutableChainInput extends ExecutableInput<ChainInput,ChainInput.Result> {

    private List<Tuple<String, ExecutableInput>> inputs;

    public ExecutableChainInput(ChainInput input, List<Tuple<String, ExecutableInput>> inputs, ESLogger logger) {
        super(input, logger);
        this.inputs = inputs;
    }

    @Override
    public ChainInput.Result execute(WatchExecutionContext ctx, Payload payload) {
        List<Tuple<String, Input.Result>> results = new ArrayList<>();
        Map<String, Object> payloads = new HashMap<>();

        try {
            for (Tuple<String, ExecutableInput> tuple : inputs) {
                Input.Result result = tuple.v2().execute(ctx, new Payload.Simple(payloads));
                results.add(new Tuple<>(tuple.v1(), result));
                payloads.put(tuple.v1(), result.payload().data());
            }

            return new ChainInput.Result(results, new Payload.Simple(payloads));
        } catch (Exception e) {
            logger.error("failed to execute [{}] input for [{}]", e, ChainInput.TYPE, ctx.watch());
            return new ChainInput.Result(e);
        }
    }
}
