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

package org.elasticsearch.xpack.watcher.transform.chain;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.xpack.watcher.execution.WatchExecutionContext;
import org.elasticsearch.xpack.watcher.transform.ExecutableTransform;
import org.elasticsearch.xpack.watcher.transform.Transform;
import org.elasticsearch.xpack.watcher.watch.Payload;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.elasticsearch.common.logging.LoggerMessageFormat.format;

/**
 *
 */
public class ExecutableChainTransform extends ExecutableTransform<ChainTransform, ChainTransform.Result> {

    private final List<ExecutableTransform> transforms;

    public ExecutableChainTransform(ChainTransform transform, ESLogger logger, ExecutableTransform... transforms) {
        this(transform, logger, Arrays.asList(transforms));
    }

    public ExecutableChainTransform(ChainTransform transform, ESLogger logger, List<ExecutableTransform> transforms) {
        super(transform, logger);
        this.transforms = Collections.unmodifiableList(transforms);
    }

    public List<ExecutableTransform> executableTransforms() {
        return transforms;
    }

    @Override
    public ChainTransform.Result execute(WatchExecutionContext ctx, Payload payload) {
        List<Transform.Result> results = new ArrayList<>();
        try {
            return doExecute(ctx, payload, results);
        } catch (Exception e) {
            logger.error("failed to execute [{}] transform for [{}]", e, ChainTransform.TYPE, ctx.id());
            return new ChainTransform.Result(e, results);
        }
    }


    ChainTransform.Result doExecute(WatchExecutionContext ctx, Payload payload, List<Transform.Result> results) throws IOException {
        for (ExecutableTransform transform : transforms) {
            Transform.Result result = transform.execute(ctx, payload);
            results.add(result);
            if (result.status() == Transform.Result.Status.FAILURE) {
                return new ChainTransform.Result(format("failed to execute [{}] transform for [{}]. failed to execute sub-transform [{}]",
                        ChainTransform.TYPE, ctx.id(), transform.type()), results);
            }
            payload = result.payload();
        }
        return new ChainTransform.Result(payload, results);
    }

}
