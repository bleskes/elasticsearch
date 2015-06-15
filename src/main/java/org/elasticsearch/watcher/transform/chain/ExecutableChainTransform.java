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

package org.elasticsearch.watcher.transform.chain;

import com.google.common.collect.ImmutableList;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.watcher.execution.WatchExecutionContext;
import org.elasticsearch.watcher.transform.ExecutableTransform;
import org.elasticsearch.watcher.transform.Transform;
import org.elasticsearch.watcher.watch.Payload;

import java.io.IOException;
import java.util.List;

/**
 *
 */
public class ExecutableChainTransform extends ExecutableTransform<ChainTransform, ChainTransform.Result> {

    private final ImmutableList<ExecutableTransform> transforms;

    public ExecutableChainTransform(ChainTransform transform, ESLogger logger, ImmutableList<ExecutableTransform> transforms) {
        super(transform, logger);
        this.transforms = transforms;
    }

    public List<ExecutableTransform> executableTransforms() {
        return transforms;
    }

    @Override
    public ChainTransform.Result execute(WatchExecutionContext ctx, Payload payload) throws IOException {
        ImmutableList.Builder<Transform.Result> results = ImmutableList.builder();
        for (ExecutableTransform transform : transforms) {
            Transform.Result result = transform.execute(ctx, payload);
            results.add(result);
            payload = result.payload();
        }
        return new ChainTransform.Result(payload, results.build());
    }

}
